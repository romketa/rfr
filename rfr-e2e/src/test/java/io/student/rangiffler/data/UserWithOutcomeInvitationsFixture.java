package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;

import java.util.ArrayList;
import java.util.List;

final class UserWithOutcomeInvitationsFixture implements UserFixture {

  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> invitees = new ArrayList<>();

  UserWithOutcomeInvitationsFixture(UserFixture delegate, int count) {
    this.delegate = delegate;
    this.count = count;
  }

  @Override
  public void create() {
    delegate.create();
    Profile me = Fixtures.profile(delegate);
    for (int i = 0; i < count; i++) {
      Profile invitee = Fixtures.profile(Fixtures.createRelated(invitees));
      Friendship.pending(me, invitee).create();
    }
  }

  @Override
  public TestUser user() {
    return delegate.user().withOutcomeInvitations(Fixtures.users(invitees));
  }

  @Override
  public void close() {
    TearDown tearDown = new TearDown();
    if (!invitees.isEmpty()) {
      tearDown.with(
          () -> Friendships.of(Fixtures.profile(delegate), FriendshipStatus.PENDING).delete());
    }
    tearDown
        .withEach(invitees, UserFixture::close)
        .with(delegate::close)
        .run();
  }
}
