package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;

import java.util.ArrayList;
import java.util.List;

final class UserWithIncomeInvitation implements UserFixture {

  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> inviters = new ArrayList<>();

  UserWithIncomeInvitation(UserFixture delegate, int count) {
    this.delegate = delegate;
    this.count = count;
  }

  @Override
  public void create() {
    delegate.create();
    Profile me = Fixtures.profile(delegate);
    for (int i = 0; i < count; i++) {
      Profile inviter = Fixtures.profile(Fixtures.createRelated(inviters));
      Friendship.pending(inviter, me).create();
    }
  }

  @Override
  public TestUser user() {
    return delegate.user().withIncomeInvitations(Fixtures.users(inviters));
  }

  @Override
  public void close() {
    TearDown tearDown = new TearDown();
    if (!inviters.isEmpty()) {
      tearDown.with(
          () -> Friendships.of(Fixtures.profile(delegate), FriendshipStatus.PENDING).delete());
    }
    tearDown
        .withEach(inviters, UserFixture::close)
        .with(delegate::close)
        .run();
  }
}
