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
    for (Relation relation : Relation.values()) {
      int count = count(relation);
      if (count > 0) {
        fixture = new UserWithRelationsFixture(fixture, relation, count);
      }
    }
    return fixture;
  }

  private int count(Relation relation) {
    return switch (relation) {
      case FRIEND -> friends;
      case INCOME_INVITATION -> incomeInvitations;
      case OUTCOME_INVITATION -> outcomeInvitations;
    };
  }
}
