package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;

import java.util.ArrayList;
import java.util.List;

final class UserWithRelationsFixture implements UserFixture {

  private final UserFixture origin;
  private final Relation relation;
  private final int count;
  private final List<UserFixture> related = new ArrayList<>();

  UserWithRelationsFixture(UserFixture origin, Relation relation, int count) {
    this.origin = origin;
    this.relation = relation;
    this.count = count;
  }

  @Override
  public void create() {
    origin.create();
    Profile me = Fixtures.profile(origin);
    for (int i = 0; i < count; i++) {
      relation.between(me, Fixtures.profile(Fixtures.createRelated(related))).create();
    }
  }

  @Override
  public TestUser user() {
    return relation.attach(origin.user(), Fixtures.users(related));
  }

  @Override
  public void close() {
    TearDown tearDown = new TearDown();
    if (!related.isEmpty()) {
      tearDown.with(() -> Friendships.of(Fixtures.profile(origin), relation.status()).delete());
    }
    tearDown
        .withEach(related, UserFixture::close)
        .with(origin::close)
        .run();
  }
}
