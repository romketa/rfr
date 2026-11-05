package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.util.DataUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.ByteBuffer;
import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.AUTH;

/**
 * A user that exists in the auth database between {@link #create()} and {@link #close()}. {@code create} inserts it,
 * bypassing the registration page, and {@link #close()} removes it together with the profile rfr-api creates on the
 * first login, so a test never leaves rows behind in either schema.
 */
public final class UserFixture implements AutoCloseable {

  // The same delegating encoder as in rfr-auth, so the user can sign in through the UI
  private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

  private TestUser user;

  /**
   * Inserts the user; a second call would orphan the first user in the database, so it is rejected.
   */
  public void create() {
    if (user != null) {
      throw new IllegalStateException("User " + user.username() + " is already created by this fixture");
    }
    user = insertIntoAuth(DataUtils.randomUsername(), DataUtils.randomPassword());
  }

  public TestUser user() {
    if (user == null) {
      throw new IllegalStateException("UserFixture is used before create() was called");
    }
    return user;
  }

  /**
   * Closing before {@link #create()} is a no-op, so the fixture can be registered for closing first and created
   * afterwards: whatever fails in between still ends with this method and no orphaned user.
   */
  @Override
  public void close() {
    if (user != null) {
      UserRecords.delete(user.username());
    }
  }

  private static TestUser insertIntoAuth(String username, String password) {
    UUID id = UUID.randomUUID();
    // One transaction, so a failure on authorities never leaves a user who can log in but is rejected by rfr-api
    Jdbc.inTransaction(AUTH, client -> {
      client.sql(Jdbc.sql("auth/insert_user")).params(toBytes(id), username, PASSWORD_ENCODER.encode(password)).update();
      client.sql(Jdbc.sql("auth/insert_authorities")).params(toBytes(id), toBytes(id)).update();
    });
    return new TestUser(id, username, password);
  }

  /**
   * Hibernate in rfr-auth stores a UUID as its 16 raw bytes, so the same layout is used here instead of UUID_TO_BIN
   * with the swap flag the column default applies.
   */
  private static byte[] toBytes(UUID id) {
    return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
  }
}
