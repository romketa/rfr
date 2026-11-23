package io.student.rangiffler.data;

public final class StaticUserFixture implements UserFixture {

  private TestUser user;

  @Override
  public void create() {
    if (user != null) {
      throw new IllegalStateException(
          "Static user " + user.username() + " is already taken by this fixture");
    }
    user = UsersQueue.take();
  }

  @Override
  public TestUser user() {
    if (user == null) {
      throw new IllegalStateException("UserFixture is used before create() was called");
    }
    return user;
  }

  @Override
  public void close() {
    if (user != null) {
      UsersQueue.release(user);
      user = null;
    }
  }
}
