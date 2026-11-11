package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.StandSchemaExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class StandSchemaExtensionTest {

  private static final String AUTH_MIGRATIONS = "rfr-auth/src/main/resources/db/migration/rangiffler-auth";
  private static final String API_MIGRATIONS = "rfr-api/src/main/resources/db/migration/rangiffler-api";

  @TempDir
  private Path root;

  @BeforeEach
  void copyCheckedOutMigrations() throws IOException {
    for (String directory : new String[]{AUTH_MIGRATIONS, API_MIGRATIONS}) {
      Path target = Files.createDirectories(root.resolve(directory));
      try (Stream<Path> files = Files.list(Config.repoRoot().resolve(directory))) {
        for (Path file : files.toList()) {
          Files.copy(file, target.resolve(file.getFileName()));
        }
      }
    }
  }

  @Test
  void shouldRunTestsWhenDatabaseMatchesCheckedOutMigrations() {
    run().testEvents().assertStatistics(stats -> stats.succeeded(1));
  }

  @Test
  void shouldFailBeforeTestsWhenDatabaseIsAheadOfCheckedOutBackend() throws IOException {
    Files.delete(root.resolve(API_MIGRATIONS).resolve("V1__schema_init.sql"));

    EngineExecutionResults results = run();

    results.testEvents().assertStatistics(stats -> stats.started(0));
    assertCallToAction(results, "[V1 (schema init)] applied to rangiffler-api but missing from",
        "check out the backend branch that has them");
  }

  @Test
  void shouldFailBeforeTestsWhenCheckedOutBackendIsAheadOfDatabase() throws IOException {
    Files.writeString(root.resolve(API_MIGRATIONS).resolve("V2__add_photos.sql"),
        "alter table `photo` add column `title` varchar(255);");

    EngineExecutionResults results = run();

    results.testEvents().assertStatistics(stats -> stats.started(0));
    assertCallToAction(results, "[V2 (add photos)] present in", "restart rfr-api so that Flyway");
  }

  @Test
  void shouldFailBeforeTestsWhenAppliedMigrationChanged() throws IOException {
    Files.writeString(root.resolve(AUTH_MIGRATIONS).resolve("V1__schema_init.sql"),
        System.lineSeparator() + "-- edited after it was applied", StandardCharsets.UTF_8,
        StandardOpenOption.APPEND);

    EngineExecutionResults results = run();

    results.testEvents().assertStatistics(stats -> stats.started(0));
    assertCallToAction(results, "[V1 (schema init)] changed in", "recreate the database with");
  }

  private EngineExecutionResults run() {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter(StandSchemaExtension.ROOT_PARAMETER, root.toString())
        .selectors(selectClass(Target.class))
        .execute();
  }

  private static void assertCallToAction(EngineExecutionResults results, String state,
      String action) {
    Throwable failure = results.containerEvents().failed().stream()
        .findFirst()
        .flatMap(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable())
        .orElseThrow(() -> new AssertionError("No container failed although the schema drifted"));
    assertInstanceOf(IllegalStateException.class, failure);
    String message = failure.getMessage();
    assertAll(
        () -> assertTrue(message.contains(state), message),
        () -> assertTrue(message.contains(action), message));
  }

  @ProbeTarget
  @ExtendWith(StandSchemaExtension.class)
  static final class Target {

    @Test
    void shouldRunOnlyWhenSchemasMatch() {
    }
  }
}
