package io.student.rangiffler.data;

import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Profile;

/**
 * Removes every row a test user leaves in rangiffler-auth and rangiffler-api. The username is the
 * only key known in both schemas, and for a user registered through the UI the test knows nothing
 * else.
 */
public final class UserRecords {

  private UserRecords() {
  }

  /**
   * Missing rows are not an error: a registration that failed leaves no auth user, and a user who
   * never logged in has no api profile. Both schemas are cleaned even when the first delete fails,
   * otherwise the api profile would survive every auth failure; the first failure is thrown and the
   * second is attached as suppressed.
   */
  public static void delete(String username) {
    new TearDown()
        .with(() -> AuthAccount.find(username).ifPresent(AuthAccount::delete))
        .with(() -> Profile.find(username).ifPresent(Profile::delete))
        .run();
  }
}
