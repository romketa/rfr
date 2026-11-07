package io.student.rangiffler.data;

public record UserRequest(TestProfile profile, int friends, int incomeInvitations,
                          int outcomeInvitations) {

  public UserRequest {
    if (friends < 0 || incomeInvitations < 0 || outcomeInvitations < 0) {
      throw new IllegalArgumentException(
          "Friends, income and outcome invitations of a test user must not be negative, but got "
              + friends + ", " + incomeInvitations + " and " + outcomeInvitations);
    }
  }

  static UserRequest withDefaultProfile() {
    return new UserRequest(TestProfile.defaults(), 0, 0, 0);
  }

  public boolean needsProfile() {
    return profile.isRequested() || friends > 0 || incomeInvitations > 0 || outcomeInvitations > 0;
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingleUserFixture(this);
    if (friends > 0) {
      fixture = new UserWithFriendsFixture(fixture, friends);
    }
    if (incomeInvitations > 0) {
      fixture = new UserWithIncomeInvitationFixture(fixture, incomeInvitations);
    }
    if (outcomeInvitations > 0) {
      fixture = new UserWithOutcomeInvitationsFixture(fixture, outcomeInvitations);
    }
    return fixture;
  }
}
