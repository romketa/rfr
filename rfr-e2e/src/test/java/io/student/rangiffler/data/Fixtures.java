package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Profile;

import java.util.List;

final class Fixtures {

  private Fixtures() {
  }

  static UserFixture createRelated(List<UserFixture> relations) {
    UserFixture related = UserRequest.withDefaultProfile().fixture();
    relations.add(related);
    related.create();
    return related;
  }

  static Profile profile(UserFixture fixture) {
    String username = fixture.user().username();
    return Profile.find(username)
        .orElseThrow(() -> new IllegalStateException(
            "Profile of " + username + " is missing from rangiffler-api"));
  }

  static List<TestUser> users(List<UserFixture> fixtures) {
    return fixtures.stream().map(UserFixture::user).toList();
  }
}
