package io.student.rangiffler.data;

public record UserRequest(TestProfile profile, int friends, int incomeInvitations) {

  public UserRequest {
    if (friends < 0 || incomeInvitations < 0) {
      throw new IllegalArgumentException(
          "Friends and income invitations of a test user must not be negative, but got "
              + friends + " and " + incomeInvitations);
    }
  }

  static UserRequest withDefaultProfile() {
    return new UserRequest(TestProfile.defaults(), 0, 0);
  }

  public boolean needsProfile() {
    return profile.isRequested() || friends > 0 || incomeInvitations > 0;
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingleUserFixture(this);
    if (friends > 0) {
      fixture = new UserWithFriends(fixture, friends);
    }
    if (incomeInvitations > 0) {
      fixture = new UserWithIncomeInvitation(fixture, incomeInvitations);
    }
    return fixture;
  }
}
