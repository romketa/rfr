package io.student.rangiffler.data;

import java.util.List;
import java.util.UUID;

/**
 * A user created in the auth database for one test. The password is kept in clear text so the test
 * can sign in.
 */
public record TestUser(UUID id, String username, String password, List<TestUser> friends,
                       List<TestUser> incomeInvitations, List<TestUser> outcomeInvitations) {

  public TestUser withFriends(List<TestUser> friends) {
    return new TestUser(id, username, password, friends, incomeInvitations, outcomeInvitations);
  }

  public TestUser withIncomeInvitations(List<TestUser> incomeInvitations) {
    return new TestUser(id, username, password, friends, incomeInvitations, outcomeInvitations);
  }

  public TestUser withOutcomeInvitations(List<TestUser> outcomeInvitations) {
    return new TestUser(id, username, password, friends, incomeInvitations, outcomeInvitations);
  }
}
