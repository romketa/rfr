package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;

import java.util.ArrayList;
import java.util.List;

final class UserWithFriendsFixture implements UserFixture {

  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> friends = new ArrayList<>();

  UserWithFriendsFixture(UserFixture delegate, int count) {
    this.delegate = delegate;
    this.count = count;
  }

  @Override
  public void create() {
    delegate.create();
    Profile me = Fixtures.profile(delegate);
    for (int i = 0; i < count; i++) {
      Profile friend = Fixtures.profile(Fixtures.createRelated(friends));
      Friendship.accepted(me, friend).create();
    }
  }

  @Override
  public TestUser user() {
    return delegate.user().withFriends(Fixtures.users(friends));
  }

  @Override
  public void close() {
    TearDown tearDown = new TearDown();
    if (!friends.isEmpty()) {
      tearDown.with(
          () -> Friendships.of(Fixtures.profile(delegate), FriendshipStatus.ACCEPTED).delete());
    }
    tearDown
        .withEach(friends, UserFixture::close)
        .with(delegate::close)
        .run();
  }
}
