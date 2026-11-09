package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class UserExtensionTest {

  @Test
  void shouldCreateUserBeforeTestAndDeleteItAfter() {
    EngineExecutionResults results = run(CreatedUserScenario.class);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    Map<String, String> published = published(results);
    assertEquals("true", published.get("authExists"));
    assertEquals("false", published.get("profileExists"));
    assertDeleted(published.get("username"));
  }

  @Test
  void shouldCreateSeparateUserForEveryParameter() {
    EngineExecutionResults results = run(TwoUsersScenario.class);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    Map<String, String> published = published(results);
    assertNotEquals(published.get("first"), published.get("second"));
    assertDeleted(published.get("first"));
    assertDeleted(published.get("second"));
  }

  @Test
  void shouldCreatePreconditionsBeforeTestAndDeleteThemAfter() {
    EngineExecutionResults results = run(PreconditionsScenario.class);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    Map<String, String> published = published(results);
    assertAll(
        () -> assertEquals("true", published.get("profileExists")),
        () -> assertEquals(2, published.get("friends").split(",").length),
        () -> assertEquals(1, published.get("inviters").split(",").length),
        () -> assertEquals(1, published.get("invitees").split(",").length),
        () -> assertEquals("true", published.get("friendsExist")),
        () -> assertEquals("true", published.get("invitationsExist")));
    Stream.of(published.get("username"), published.get("friends"), published.get("inviters"),
            published.get("invitees"))
        .flatMap(usernames -> Stream.of(usernames.split(",")))
        .forEach(UserExtensionTest::assertDeleted);
  }

  @Test
  void shouldCreatePendingInvitationToInviteeAndDeleteItAfter() {
    EngineExecutionResults results = run(OutcomeInvitationScenario.class);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    Map<String, String> published = published(results);
    assertEquals("PENDING", published.get("invitationStatus"));
    assertTrue(Friendship.find(UUID.fromString(published.get("invitationId"))).isEmpty(),
        "Invitation " + published.get("invitationId") + " is left in rangiffler-api");
    assertDeleted(published.get("invitee"));
  }

  @Test
  void shouldRejectNegativeOutcomeInvitationsBeforeCreatingUser() {
    EngineExecutionResults results = run(NegativeCountScenario.class);

    results.testEvents().assertStatistics(stats -> stats.failed(1));
    Throwable failure = failure(results);
    assertInstanceOf(IllegalArgumentException.class, failure);
    assertTrue(failure.getMessage().contains("must not be negative"), failure.getMessage());
    assertTrue(published(results).isEmpty(), "The test body ran although its request was invalid");
  }

  @Test
  void shouldDeleteUserWhenTestFails() {
    EngineExecutionResults results = run(FailingScenario.class);

    results.testEvents().assertStatistics(stats -> stats.failed(1));
    assertDeleted(published(results).get("username"));
  }

  @Test
  void shouldFailTestWhenUserCannotBeCreated() {
    EngineExecutionResults results = run(UnknownCountryScenario.class);

    results.testEvents().assertStatistics(stats -> stats.failed(1));
    Throwable failure = failure(results);
    assertInstanceOf(IllegalStateException.class, failure);
    assertTrue(failure.getMessage().contains("country zz is missing"), failure.getMessage());
    assertTrue(published(results).isEmpty(), "The test body ran although its user was not created");
  }

  @Test
  void shouldNotSupportParameterOfLifecycleMethod() {
    assertNotSupported(run(LifecycleParameterScenario.class));
  }

  @Test
  void shouldNotSupportParameterOfOtherType() {
    assertNotSupported(run(WrongTypeScenario.class));
  }

  @Test
  void shouldNotSupportTestUserWithoutAnnotation() {
    assertNotSupported(run(NoAnnotationScenario.class));
  }

  private static void assertNotSupported(EngineExecutionResults results) {
    results.testEvents().assertStatistics(stats -> stats.failed(1));
    Throwable failure = failure(results);
    assertInstanceOf(ParameterResolutionException.class, failure);
    assertTrue(failure.getMessage().startsWith("No ParameterResolver registered"),
        failure.getMessage());
  }

  private static EngineExecutionResults run(Class<?> scenario) {
    return EngineTestKit.engine("junit-jupiter").selectors(selectClass(scenario)).execute();
  }

  private static Map<String, String> published(EngineExecutionResults results) {
    return results.allEvents().reportingEntryPublished().stream()
        .flatMap(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs().entrySet()
            .stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Throwable failure(EngineExecutionResults results) {
    return results.testEvents().failed().stream()
        .findFirst()
        .flatMap(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable())
        .orElseThrow(() -> new AssertionError("The scenario has no failed test with a throwable"));
  }

  private static void assertDeleted(String username) {
    assertAll(
        () -> assertTrue(AuthAccount.find(username).isEmpty(),
            "User " + username + " is left in rangiffler-auth"),
        () -> assertTrue(Profile.find(username).isEmpty(),
            "Profile of " + username + " is left in rangiffler-api"));
  }

  private static String usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).collect(Collectors.joining(","));
  }

  @Nested
  @Tag("engine-kit")
  class CreatedUserScenario {

    @Test
    void shouldPublishCreatedUser(@User TestUser user, TestReporter reporter) {
      reporter.publishEntry(Map.of(
          "username", user.username(),
          "authExists", String.valueOf(AuthAccount.find(user.username()).isPresent()),
          "profileExists", String.valueOf(Profile.find(user.username()).isPresent())));
    }
  }

  @Nested
  @Tag("engine-kit")
  class TwoUsersScenario {

    @Test
    void shouldPublishBothUsers(@User TestUser first, @User TestUser second,
        TestReporter reporter) {
      reporter.publishEntry(Map.of("first", first.username(), "second", second.username()));
    }
  }

  @Nested
  @Tag("engine-kit")
  class OutcomeInvitationScenario {

    @Test
    void shouldPublishInvitation(@User(outcomeInvitations = 1) TestUser user,
        TestReporter reporter) {
      TestUser invitee = user.outcomeInvitations().getFirst();
      Friendship invitation = Friendship.find(Profile.find(user.username()).orElseThrow(),
          Profile.find(invitee.username()).orElseThrow()).orElseThrow();
      reporter.publishEntry(Map.of(
          "invitee", invitee.username(),
          "invitationId", invitation.id().toString(),
          "invitationStatus", invitation.status().name()));
    }
  }

  @Nested
  @Tag("engine-kit")
  class NegativeCountScenario {

    @Test
    void shouldNotRun(@User(outcomeInvitations = -1) TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
    }
  }

  @Nested
  @Tag("engine-kit")
  class PreconditionsScenario {

    @Test
    void shouldPublishPreconditions(
        @User(friends = 2, incomeInvitations = 1, outcomeInvitations = 1) TestUser user,
        TestReporter reporter) {
      Profile profile = Profile.find(user.username()).orElseThrow();
      reporter.publishEntry(Map.of(
          "username", user.username(),
          "profileExists", String.valueOf(profile.exists()),
          "friends", usernames(user.friends()),
          "inviters", usernames(user.incomeInvitations()),
          "invitees", usernames(user.outcomeInvitations()),
          "friendsExist",
          String.valueOf(Friendships.of(profile, FriendshipStatus.ACCEPTED).exists()),
          "invitationsExist",
          String.valueOf(Friendships.of(profile, FriendshipStatus.PENDING).exists())));
    }
  }

  @Nested
  @Tag("engine-kit")
  class FailingScenario {

    @Test
    void shouldFailAfterPublishingUser(@User TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
      fail("The scenario fails on purpose");
    }
  }

  @Nested
  @Tag("engine-kit")
  class UnknownCountryScenario {

    @Test
    void shouldNotRun(@User(country = "zz") TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
    }
  }

  @Nested
  @Tag("engine-kit")
  class LifecycleParameterScenario {

    @BeforeEach
    void setUp(@User TestUser user) {
    }

    @Test
    void shouldNotRun() {
    }
  }

  @Nested
  @Tag("engine-kit")
  class WrongTypeScenario {

    @Test
    void shouldNotRun(@User String user) {
    }
  }

  @Nested
  @Tag("engine-kit")
  class NoAnnotationScenario {

    @Test
    void shouldNotRun(TestUser user) {
    }
  }
}
