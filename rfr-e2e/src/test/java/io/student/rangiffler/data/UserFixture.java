package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.util.DataUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.ByteBuffer;
import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.API;
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
   * Both schemas are cleaned even when the first delete fails: the api profile would otherwise survive every auth
   * failure. Like try-with-resources, the first failure is thrown and the second is attached as suppressed.
   */
  @Override
  public void close() {
    if (user == null) {
      return;
    }
    try {
      deleteFromAuth();
    } catch (RuntimeException authFailure) {
      try {
        deleteFromApi();
      } catch (RuntimeException apiFailure) {
        authFailure.addSuppressed(apiFailure);
      }
      throw authFailure;
    }
    deleteFromApi();
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

  private void deleteFromAuth() {
    Jdbc.inTransaction(AUTH, client -> {
      client.sql(Jdbc.sql("auth/delete_authorities")).param(toBytes(user.id())).update();
      client.sql(Jdbc.sql("auth/delete_user")).param(toBytes(user.id())).update();
    });
  }

  /**
   * The only link between the two schemas is the username, so the profile is looked up by it, not by the auth id.
   * Everything that references the user goes before the user itself, in foreign-key order: the photo, like and
   * statistic tables are empty while the API mocks them, but the fixture must keep working once they are persisted.
   * A user who never logged in has no profile, so deleting nothing is not an error.
   */
  private void deleteFromApi() {
    Jdbc.inTransaction(API, client ->
        client.sql(Jdbc.sql("api/select_user_id")).param(user.username()).query(byte[].class).optional().ifPresent(id -> {
          client.sql(Jdbc.sql("api/delete_friendships")).params(id, id).update();
          client.sql(Jdbc.sql("api/delete_photo_likes")).params(id, id).update();
          client.sql(Jdbc.sql("api/delete_likes")).param(id).update();
          client.sql(Jdbc.sql("api/delete_photos")).param(id).update();
          client.sql(Jdbc.sql("api/delete_statistics")).param(id).update();
          client.sql(Jdbc.sql("api/delete_user")).param(id).update();
        }));
  }

  /**
   * Hibernate in rfr-auth stores a UUID as its 16 raw bytes, so the same layout is used here instead of UUID_TO_BIN
   * with the swap flag the column default applies.
   */
  private static byte[] toBytes(UUID id) {
    return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
  }
}
