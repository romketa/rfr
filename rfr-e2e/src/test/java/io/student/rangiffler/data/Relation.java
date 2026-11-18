package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.Profile;

import java.util.List;
import java.util.function.BiFunction;

enum Relation {
  FRIEND(Friendship::accepted, TestUser::withFriends),
  INCOME_INVITATION((me, inviter) -> Friendship.pending(inviter, me),
      TestUser::withIncomeInvitations),
  OUTCOME_INVITATION(Friendship::pending, TestUser::withOutcomeInvitations);

  private final BiFunction<Profile, Profile, Friendship> link;
  private final BiFunction<TestUser, List<TestUser>, TestUser> attach;

  Relation(BiFunction<Profile, Profile, Friendship> link,
      BiFunction<TestUser, List<TestUser>, TestUser> attach) {
    this.link = link;
    this.attach = attach;
  }

  Friendship between(Profile me, Profile other) {
    return link.apply(me, other);
  }

  TestUser attach(TestUser user, List<TestUser> related) {
    return attach.apply(user, related);
  }
}
