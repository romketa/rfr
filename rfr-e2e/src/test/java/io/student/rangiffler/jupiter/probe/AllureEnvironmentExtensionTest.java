package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class AllureEnvironmentExtensionTest {

  @TempDir
  private Path results;

  @Test
  void shouldWriteEnvironmentWithAllKeysAfterRun() throws IOException {
    run(Target.class).testEvents().assertStatistics(stats -> stats.succeeded(1));

    Map<String, String> environment = environment();
    assertAll(
        () -> assertEquals(AllureEnvironmentExtension.KEYS, environment.keySet().stream().toList()),
        () -> assertEquals(Config.frontUrl(), environment.get("frontUrl")),
        () -> assertEquals(Config.commit(), environment.get("commit")),
        () -> assertTrue(environment.get("authSchemaVersion").matches("\\d+"),
            environment.toString()),
        () -> assertTrue(environment.get("apiSchemaVersion").matches("\\d+"),
            environment.toString()),
        () -> assertTrue(environment.values().stream().noneMatch(String::isEmpty),
            environment.toString()));
  }

  @Test
  void shouldWriteEnvironmentWhenAnotherBeforeSuiteFails() throws IOException {
    EngineExecutionResults run = run(BrokenTarget.class);

    run.testEvents().assertStatistics(stats -> stats.started(0));
    assertEquals(AllureEnvironmentExtension.KEYS, environment().keySet().stream().toList());
  }

  @Test
  void shouldNotReachLaterBeforeAllAfterFailedOne() {
    Counting.CALLS.set(0);

    run(BrokenThenCountingTarget.class).testEvents().assertStatistics(stats -> stats.started(0));

    assertEquals(0, Counting.CALLS.get(), "beforeAll of the extension after the failed one ran");
  }

  private EngineExecutionResults run(Class<?> target) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
        .configurationParameter(AllureEnvironmentExtension.RESULTS_PARAMETER, results.toString())
        .selectors(selectClass(target))
        .execute();
  }

  private Map<String, String> environment() throws IOException {
    Path file = results.resolve(AllureEnvironmentExtension.FILE_NAME);
    assertTrue(Files.exists(file), file + " was not written");
    return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
        .map(line -> line.split("=", 2))
        .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1], (a, b) -> a,
            java.util.LinkedHashMap::new));
  }

  static final class Broken implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
      throw new IllegalStateException("beforeSuite of Broken fails on purpose");
    }
  }

  static final class Counting implements SuiteExtension {

    static final AtomicInteger CALLS = new AtomicInteger();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
      CALLS.incrementAndGet();
      SuiteExtension.super.beforeAll(context);
    }
  }

  @ProbeTarget
  static final class Target {

    @Test
    void shouldRunAndLeaveTheEnvironmentBehind() {
    }
  }

  @ProbeTarget
  @ExtendWith(Broken.class)
  static final class BrokenTarget {

    @Test
    void shouldNotRun() {
    }
  }

  @ProbeTarget
  @ExtendWith({Broken.class, Counting.class})
  static final class BrokenThenCountingTarget {

    @Test
    void shouldNotRun() {
    }
  }
}
