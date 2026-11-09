package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UsersQueue;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class UsersQueueExtensionTest {

  private static final int POOL_SIZE = 3;

  @Test
  void shouldServeFourTestsInARowFromPoolOfThree() {
    EngineExecutionResults results = run(FourTestsScenario.class);

    results.testEvents().assertStatistics(stats -> stats.succeeded(4));
    List<Map<String, String>> entries = published(results);
    assertEquals(4, entries.size());
    assertAll(entries.stream().map(entry -> () -> assertEquals(
        String.valueOf(POOL_SIZE - 1), entry.get("available"),
        "Test with " + entry.get("username") + " did not hold exactly one static user")));
    assertEquals(POOL_SIZE, UsersQueue.available(), "A static user is not returned to the queue");
  }

  @Test
  void shouldReturnUserToQueueAfterFailedTest() {
    EngineExecutionResults results = run(FailingScenario.class);

    results.testEvents().assertStatistics(stats -> stats.failed(1));
    Map<String, String> entry = published(results).getFirst();
    assertTrue(entry.get("username").startsWith("rfr_static_"), entry.get("username"));
    assertEquals(String.valueOf(POOL_SIZE - 1), entry.get("available"));
    assertEquals(POOL_SIZE, UsersQueue.available(),
        "Static user " + entry.get("username") + " is not returned after the failed test");
  }

  private static EngineExecutionResults run(Class<?> scenario) {
    return EngineTestKit.engine("junit-jupiter").selectors(selectClass(scenario)).execute();
  }

  private static List<Map<String, String>> published(EngineExecutionResults results) {
    return results.allEvents().reportingEntryPublished().stream()
        .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
        .toList();
  }

  private static void publish(TestReporter reporter, TestUser user) {
    reporter.publishEntry(Map.of(
        "username", user.username(),
        "available", String.valueOf(UsersQueue.available())));
  }

  @Nested
  @Tag("engine-kit")
  class FourTestsScenario {

    @Test
    void shouldPublishFirstUser(@StaticUser TestUser user, TestReporter reporter) {
      publish(reporter, user);
    }

    @Test
    void shouldPublishSecondUser(@StaticUser TestUser user, TestReporter reporter) {
      publish(reporter, user);
    }

    @Test
    void shouldPublishThirdUser(@StaticUser TestUser user, TestReporter reporter) {
      publish(reporter, user);
    }

    @Test
    void shouldPublishFourthUser(@StaticUser TestUser user, TestReporter reporter) {
      publish(reporter, user);
    }
  }

  @Nested
  @Tag("engine-kit")
  class FailingScenario {

    @Test
    void shouldFailAfterPublishingUser(@StaticUser TestUser user, TestReporter reporter) {
      publish(reporter, user);
      fail("The scenario fails on purpose");
    }
  }
}
