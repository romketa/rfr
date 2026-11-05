package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Jdbc;

import static io.student.rangiffler.data.db.Databases.API;
import static io.student.rangiffler.data.db.Databases.AUTH;

/**
 * Removes every row a test user leaves in rangiffler-auth and rangiffler-api. The username is the only key known in
 * both schemas, and for a user registered through the UI the test knows nothing else.
 */
public final class UserRecords {

  private UserRecords() {
  }

  /**
   * Missing rows are not an error: a registration that failed leaves no auth user, and a user who never logged in has
   * no api profile. Both schemas are cleaned even when the first delete fails, otherwise the api profile would survive
   * every auth failure; like try-with-resources, the first failure is thrown and the second is attached as suppressed.
   */
  public static void delete(String username) {
    try {
      deleteFromAuth(username);
    } catch (RuntimeException authFailure) {
      try {
        deleteFromApi(username);
      } catch (RuntimeException apiFailure) {
        authFailure.addSuppressed(apiFailure);
      }
      throw authFailure;
    }
    deleteFromApi(username);
  }

  private static void deleteFromAuth(String username) {
    Jdbc.inTransaction(AUTH, client ->
        client.sql(Jdbc.sql("auth/select_user_id")).param(username).query(byte[].class).optional().ifPresent(id -> {
          client.sql(Jdbc.sql("auth/delete_authorities")).param(id).update();
          client.sql(Jdbc.sql("auth/delete_user")).param(id).update();
        }));
  }

  /**
   * Everything that references the user goes before the user itself, in foreign-key order: the photo, like and
   * statistic tables are empty while the API mocks them, but the cleanup must keep working once they are persisted.
   */
  private static void deleteFromApi(String username) {
    Jdbc.inTransaction(API, client ->
        client.sql(Jdbc.sql("api/select_user_id")).param(username).query(byte[].class).optional().ifPresent(id -> {
          client.sql(Jdbc.sql("api/delete_friendships")).params(id, id).update();
          client.sql(Jdbc.sql("api/delete_photo_likes")).params(id, id).update();
          client.sql(Jdbc.sql("api/delete_likes")).param(id).update();
          client.sql(Jdbc.sql("api/delete_photos")).param(id).update();
          client.sql(Jdbc.sql("api/delete_statistics")).param(id).update();
          client.sql(Jdbc.sql("api/delete_user")).param(id).update();
        }));
  }
}
