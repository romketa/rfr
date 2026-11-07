package io.student.rangiffler.data;

import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.util.DataUtils;

import java.util.List;

/**
 * A user that exists in the auth database between {@link #create()} and {@link #close()}.
 * {@code create} inserts it, bypassing the registration page, and {@link #close()} removes it
 * together with the profile rfr-api creates on the first login, so a test never leaves rows behind
 * in either schema.
 */
public final class SingleUserFixture implements UserFixture {

  private final UserRequest request;
  private AuthAccount account;
  private TestUser user;

  SingleUserFixture(UserRequest request) {
    this.request = request;
  }

  /**
   * Inserts the user; a second call would orphan the first user in the database, so it is
   * rejected.
   */
  @Override
  public void create() {
    if (user != null) {
      throw new IllegalStateException(
          "User " + user.username() + " is already created by this fixture");
    }
    String username = DataUtils.randomUsername();
    String password = DataUtils.randomPassword();
    account = AuthAccount.random(username, password).create();
    user = new TestUser(account.id(), username, password, List.of(), List.of());
    if (request.needsProfile()) {
      TestProfile profile = request.profile();
      Profile.random(username, profile.firstname(), profile.surname(), profile.countryOrDefault())
          .create();
    }
  }

  @Override
  public TestUser user() {
    if (user == null) {
      throw new IllegalStateException("UserFixture is used before create() was called");
    }
    return user;
  }

  /**
   * Closing before {@link #create()} is a no-op, so the fixture can be registered for closing first
   * and created afterwards: whatever fails in between still ends with this method and no orphaned
   * user.
   */
  @Override
  public void close() {
    if (user != null) {
      new TearDown()
          .with(account::delete)
          .with(() -> Profile.find(user.username()).ifPresent(Profile::delete))
          .run();
    }
  }
}
