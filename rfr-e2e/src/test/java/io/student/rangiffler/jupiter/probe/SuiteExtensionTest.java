package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.condition.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("engine-probe")
@DisplayName("SuiteExtension")
class SuiteExtensionTest {

  @Test
  @DisplayName("beforeSuite вызывается один раз на несколько классов")
  void beforeSuiteRunsOnceForSeveralClasses(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RecordedFirst.class, RecordedSecond.class, RecordedThird.class);
    assertEquals(1, Collections.frequency(journal.events(), "beforeSuite Recording"),
        "beforeSuite was not called exactly once for three classes");
  }

  @Test
  @DisplayName("afterSuite вызывается один раз на несколько классов")
  void afterSuiteRunsOnceForSeveralClasses(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RecordedFirst.class, RecordedSecond.class, RecordedThird.class);
    assertEquals(1, Collections.frequency(journal.events(), "afterSuite Recording"),
        "afterSuite was not called exactly once for three classes");
  }

  @Test
  @DisplayName("beforeSuite выполняется раньше любого теста")
  void beforeSuitePrecedesEveryTest(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RecordedFirst.class, RecordedSecond.class, RecordedThird.class);
    List<String> events = journal.events();
    assertTrue(events.indexOf("beforeSuite Recording") >= 0
            && events.indexOf("beforeSuite Recording") < firstTest(events),
        "beforeSuite did not run before the first test");
  }

  @Test
  @DisplayName("afterSuite выполняется после всех тестов")
  void afterSuiteFollowsEveryTest(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RecordedFirst.class, RecordedSecond.class, RecordedThird.class);
    List<String> events = journal.events();
    assertTrue(events.indexOf("afterSuite Recording") > lastTest(events),
        "afterSuite did not run after the last test");
  }

  @Test
  @DisplayName("Вложенный класс не вызывает beforeSuite повторно")
  void beforeSuiteRunsOnceWithNestedClass(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RecordedOuter.class);
    assertEquals(1, Collections.frequency(journal.events(), "beforeSuite Recording"),
        "beforeSuite was called again for the nested class");
  }

  @Test
  @DisplayName("beforeSuite получает корневой контекст")
  void beforeSuiteGetsRootContext(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RootedFirst.class);
    assertEquals(List.of("beforeSuite root true"),
        journal.events().stream().filter(event -> event.startsWith("beforeSuite")).toList(),
        "beforeSuite did not get the root context");
  }

  @Test
  @DisplayName("afterSuite получает корневой контекст")
  void afterSuiteGetsRootContext(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RootedFirst.class);
    assertEquals(List.of("afterSuite root true"),
        journal.events().stream().filter(event -> event.startsWith("afterSuite")).toList(),
        "afterSuite did not get the root context");
  }

  @Test
  @DisplayName("Каждый запуск получает свой beforeSuite")
  void eachRunGetsItsOwnBeforeSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), RecordedFirst.class);
    run(journal, Map.of(), RecordedFirst.class);
    assertEquals(2, Collections.frequency(journal.events(), "beforeSuite Recording"),
        "Two runs did not get one beforeSuite each");
  }

  @Test
  @DisplayName("Каждая реализация получает свой beforeSuite")
  void eachImplementationGetsItsOwnBeforeSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), TwinnedFirst.class, TwinnedSecond.class);
    assertEquals(1, Collections.frequency(journal.events(), "beforeSuite Twin"),
        "Second implementation did not get exactly one beforeSuite");
  }

  @Test
  @DisplayName("При параллельном запуске beforeSuite вызывается один раз")
  void beforeSuiteRunsOnceInParallel(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, parallel(), SlowFirst.class, SlowSecond.class, SlowThird.class);
    assertEquals(1, Collections.frequency(journal.events(), "beforeSuite started Slow"),
        "beforeSuite was not called exactly once in a parallel run");
  }

  @Test
  @DisplayName("При параллельном запуске тесты ждут окончания beforeSuite")
  void parallelTestsWaitForBeforeSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, parallel(), SlowFirst.class, SlowSecond.class, SlowThird.class);
    List<String> events = journal.events();
    assertTrue(events.indexOf("beforeSuite finished Slow") >= 0
            && events.indexOf("beforeSuite finished Slow") < firstTest(events),
        "A parallel test started before beforeSuite finished");
  }

  @Test
  @DisplayName("Упавший beforeSuite не вызывается повторно")
  void failedBeforeSuiteIsNotRetried(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), BrokenBeforeFirst.class, BrokenBeforeSecond.class);
    assertEquals(1, Collections.frequency(journal.events(), "beforeSuite BrokenBefore"),
        "Failed beforeSuite was called again");
  }

  @Test
  @DisplayName("Упавший beforeSuite валит каждый класс своим исключением")
  void failedBeforeSuiteFailsEveryClass(@TempDir Path dir) {
    EngineExecutionResults results = run(
        new Journal(dir.resolve("journal")), Map.of(), BrokenBeforeFirst.class,
        BrokenBeforeSecond.class);
    assertEquals(2, failures(results, "beforeSuite of BrokenBefore fails on purpose"),
        "Failed beforeSuite did not fail both classes with its exception");
  }

  @Test
  @DisplayName("Упавший beforeSuite не пускает тесты")
  void failedBeforeSuiteSkipsTests(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), BrokenBeforeFirst.class, BrokenBeforeSecond.class);
    assertEquals(-1, firstTest(journal.events()), "A test ran after beforeSuite failed");
  }

  @Test
  @DisplayName("После упавшего beforeSuite afterSuite всё равно вызывается")
  void failedBeforeSuiteStillRunsAfterSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), BrokenBeforeFirst.class, BrokenBeforeSecond.class);
    assertEquals(1, Collections.frequency(journal.events(), "afterSuite BrokenBefore"),
        "afterSuite was not called exactly once after beforeSuite failed");
  }

  @Test
  @DisplayName("Упавший afterSuite вызывается один раз")
  void failedAfterSuiteIsNotRetried(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), BrokenAfterFirst.class, BrokenAfterSecond.class);
    assertEquals(1, Collections.frequency(journal.events(), "afterSuite BrokenAfter"),
        "Failed afterSuite was not called exactly once");
  }

  @Test
  @DisplayName("Упавший afterSuite попадает в отчёт")
  void failedAfterSuiteIsReported(@TempDir Path dir) {
    EngineExecutionResults results = run(
        new Journal(dir.resolve("journal")), Map.of(), BrokenAfterFirst.class,
        BrokenAfterSecond.class);
    assertEquals(1, failures(results, "afterSuite of BrokenAfter fails on purpose"),
        "Failed afterSuite was not reported exactly once");
  }

  @Test
  @DisplayName("Упавший afterSuite не валит прошедшие тесты")
  void failedAfterSuiteKeepsTestsGreen(@TempDir Path dir) {
    EngineExecutionResults results = run(
        new Journal(dir.resolve("journal")), Map.of(), BrokenAfterFirst.class,
        BrokenAfterSecond.class);
    assertEquals(2L, results.testEvents().succeeded().count(),
        "Failed afterSuite turned passed tests red");
  }

  @Test
  @DisplayName("Упавший afterSuite одной реализации не мешает afterSuite другой")
  void failedAfterSuiteDoesNotBlockAnotherAfterSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), BrokenAfterWithRecording.class);
    assertEquals(1, Collections.frequency(journal.events(), "afterSuite Recording"),
        "Failed afterSuite of one implementation blocked afterSuite of another");
  }

  @Test
  @DisplayName("Ошибка beforeSuite прикрепляется к ошибке afterSuite как suppressed")
  void failedBeforeSuiteIsSuppressedByFailedAfterSuite(@TempDir Path dir) {
    EngineExecutionResults results = run(new Journal(dir.resolve("journal")), Map.of(),
        BrokenBothFirst.class);
    assertEquals(1L, results.containerEvents().failed().stream()
            .map(event -> event.getRequiredPayload(TestExecutionResult.class))
            .flatMap(result -> result.getThrowable().stream())
            .flatMap(
                throwable -> Stream.iterate(throwable, cause -> cause != null, Throwable::getCause))
            .filter(
                throwable -> "afterSuite of BrokenBoth fails on purpose".equals(throwable.getMessage()))
            .filter(throwable -> Arrays.stream(throwable.getSuppressed())
                .anyMatch(suppressed -> "beforeSuite of BrokenBoth fails on purpose".equals(
                    suppressed.getMessage())))
            .count(),
        "Failed afterSuite did not carry the beforeSuite failure as suppressed");
  }

  @Test
  @DisplayName("Упавший тест не мешает afterSuite")
  void failedTestStillRunsAfterSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), FailingTest.class);
    assertEquals(1, Collections.frequency(journal.events(), "afterSuite Recording"),
        "afterSuite was not called after a failed test");
  }

  @Test
  @DisplayName("Упавший @AfterAll не мешает afterSuite")
  void failedAfterAllStillRunsAfterSuite(@TempDir Path dir) {
    Journal journal = new Journal(dir.resolve("journal"));
    run(journal, Map.of(), FailingAfterAll.class);
    assertEquals(1, Collections.frequency(journal.events(), "afterSuite Recording"),
        "afterSuite was not called after a failed @AfterAll");
  }

  private EngineExecutionResults run(Journal journal, Map<String, String> parameters,
      Class<?>... targets) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter(ProbeTargetCondition.PARAMETER, "true")
        .configurationParameter(Journal.PARAMETER, journal.path().toString())
        .configurationParameters(parameters)
        .selectors(Arrays.stream(targets).map(DiscoverySelectors::selectClass)
            .toArray(DiscoverySelector[]::new))
        .execute();
  }

  private Map<String, String> parallel() {
    return Map.of(
        "junit.jupiter.execution.parallel.enabled", "true",
        "junit.jupiter.execution.parallel.mode.default", "concurrent",
        "junit.jupiter.execution.parallel.mode.classes.default", "concurrent",
        "junit.jupiter.execution.parallel.config.strategy", "fixed",
        "junit.jupiter.execution.parallel.config.fixed.parallelism", "3");
  }

  private int firstTest(List<String> events) {
    return events.stream().filter(event -> event.startsWith("test ")).findFirst()
        .map(events::indexOf).orElse(-1);
  }

  private int lastTest(List<String> events) {
    return events.stream().filter(event -> event.startsWith("test ")).map(events::lastIndexOf)
        .max(Integer::compare)
        .orElse(-1);
  }

  private long failures(EngineExecutionResults results, String message) {
    return results.containerEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(result -> result.getThrowable().stream())
        .filter(throwable -> Stream.iterate(throwable, cause -> cause != null, Throwable::getCause)
            .anyMatch(cause -> message.equals(cause.getMessage())))
        .count();
  }

  record Journal(Path path) {

    static final String PARAMETER = "rfr.probe.journal";

    Journal(ExtensionContext context) {
      this(Path.of(context.getConfigurationParameter(PARAMETER).orElseThrow(
          () -> new IllegalStateException(
              "Configuration parameter " + PARAMETER + " is missing"))));
    }

    void write(String event) {
      try {
        Files.writeString(path, event + System.lineSeparator(), StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      } catch (IOException exception) {
        throw new UncheckedIOException("Cannot write event " + event + " to journal " + path,
            exception);
      }
    }

    List<String> events() {
      try {
        return Files.exists(path) ? Files.readAllLines(path) : List.of();
      } catch (IOException exception) {
        throw new UncheckedIOException("Cannot read journal " + path, exception);
      }
    }
  }

  static final class JournalResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
      return parameter.getParameter().getType() == Journal.class;
    }

    @Override
    public Journal resolveParameter(ParameterContext parameter, ExtensionContext context) {
      return new Journal(context);
    }
  }

  static final class Recording implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite Recording");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite Recording");
    }
  }

  static final class Rooted implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite root " + context.getParent().isEmpty());
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite root " + context.getParent().isEmpty());
    }
  }

  static final class Twin implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite Twin");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite Twin");
    }
  }

  static final class Slow implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite started Slow");
      try {
        Thread.sleep(300);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("beforeSuite of Slow was interrupted", exception);
      }
      new Journal(context).write("beforeSuite finished Slow");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite Slow");
    }
  }

  static final class BrokenBefore implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite BrokenBefore");
      throw new IllegalStateException("beforeSuite of BrokenBefore fails on purpose");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite BrokenBefore");
    }
  }

  static final class BrokenAfter implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite BrokenAfter");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite BrokenAfter");
      throw new IllegalStateException("afterSuite of BrokenAfter fails on purpose");
    }
  }

  static final class BrokenBoth implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      new Journal(context).write("beforeSuite BrokenBoth");
      throw new IllegalStateException("beforeSuite of BrokenBoth fails on purpose");
    }

    @Override
    public void afterSuite(ExtensionContext context) {
      new Journal(context).write("afterSuite BrokenBoth");
      throw new IllegalStateException("afterSuite of BrokenBoth fails on purpose");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, JournalResolver.class})
  static final class RecordedFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test RecordedFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, JournalResolver.class})
  static final class RecordedSecond {

    @Test
    void observe(Journal journal) {
      journal.write("test RecordedSecond");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, JournalResolver.class})
  static final class RecordedThird {

    @Test
    void observe(Journal journal) {
      journal.write("test RecordedThird");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, JournalResolver.class})
  static final class RecordedOuter {

    @Test
    void observe(Journal journal) {
      journal.write("test RecordedOuter");
    }

    @Nested
    final class Inner {

      @Test
      void observe(Journal journal) {
        journal.write("test RecordedOuter.Inner");
      }
    }
  }

  @ProbeTarget
  @ExtendWith({Rooted.class, JournalResolver.class})
  static final class RootedFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test RootedFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, Twin.class, JournalResolver.class})
  static final class TwinnedFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test TwinnedFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, Twin.class, JournalResolver.class})
  static final class TwinnedSecond {

    @Test
    void observe(Journal journal) {
      journal.write("test TwinnedSecond");
    }
  }

  @ProbeTarget
  @ExtendWith({Slow.class, JournalResolver.class})
  static final class SlowFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test SlowFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({Slow.class, JournalResolver.class})
  static final class SlowSecond {

    @Test
    void observe(Journal journal) {
      journal.write("test SlowSecond");
    }
  }

  @ProbeTarget
  @ExtendWith({Slow.class, JournalResolver.class})
  static final class SlowThird {

    @Test
    void observe(Journal journal) {
      journal.write("test SlowThird");
    }
  }

  @ProbeTarget
  @ExtendWith({BrokenBefore.class, JournalResolver.class})
  static final class BrokenBeforeFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test BrokenBeforeFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({BrokenBefore.class, JournalResolver.class})
  static final class BrokenBeforeSecond {

    @Test
    void observe(Journal journal) {
      journal.write("test BrokenBeforeSecond");
    }
  }

  @ProbeTarget
  @ExtendWith({BrokenAfter.class, JournalResolver.class})
  static final class BrokenAfterFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test BrokenAfterFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({BrokenAfter.class, JournalResolver.class})
  static final class BrokenAfterSecond {

    @Test
    void observe(Journal journal) {
      journal.write("test BrokenAfterSecond");
    }
  }

  @ProbeTarget
  @ExtendWith({BrokenBoth.class, JournalResolver.class})
  static final class BrokenBothFirst {

    @Test
    void observe(Journal journal) {
      journal.write("test BrokenBothFirst");
    }
  }

  @ProbeTarget
  @ExtendWith({BrokenAfter.class, Recording.class, JournalResolver.class})
  static final class BrokenAfterWithRecording {

    @Test
    void observe(Journal journal) {
      journal.write("test BrokenAfterWithRecording");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, JournalResolver.class})
  static final class FailingTest {

    @Test
    void observe(Journal journal) {
      journal.write("test FailingTest");
      throw new IllegalStateException("Probe target fails on purpose");
    }
  }

  @ProbeTarget
  @ExtendWith({Recording.class, JournalResolver.class})
  static final class FailingAfterAll {

    @AfterAll
    static void fail() {
      throw new IllegalStateException("Probe target fails in @AfterAll on purpose");
    }

    @Test
    void observe(Journal journal) {
      journal.write("test FailingAfterAll");
    }
  }
}