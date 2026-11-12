package io.student.rangiffler.data.db;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.API;

public final class Profile implements Row {

  private final UUID id;
  private final String username;
  private final String firstname;
  private final String surname;
  private final String countryCode;

  private Profile(UUID id, String username, String firstname, String surname, String countryCode) {
    this.id = id;
    this.username = username;
    this.firstname = firstname;
    this.surname = surname;
    this.countryCode = countryCode;
  }

  public static Profile random(String username, String firstname, String surname,
      String countryCode) {
    return new Profile(UUID.randomUUID(), username, firstname, surname, countryCode);
  }

  public static Optional<Profile> find(String username) {
    return Jdbc.client(API).sql(Jdbc.sql("api/select_user")).param(username)
        .query((rs, rowNum) -> new Profile(UUID.fromString(rs.getString("id")), username,
            Objects.toString(rs.getString("firstname"), ""),
            Objects.toString(rs.getString("last_name"), ""),
            rs.getString("code")))
        .optional();
  }

  UUID id() {
    return id;
  }

  @Override
  public Profile create() {
    int inserted = Jdbc.client(API).sql(Jdbc.sql("api/create_profile"))
        .params(id.toString(), username, firstname, surname, countryCode)
        .update();
    if (inserted != 1) {
      throw new IllegalStateException("Profile of " + username + " was not created because country "
          + countryCode + " is missing from rangiffler-api");
    }
    return this;
  }

  @Override
  public void delete() {
    String id = this.id.toString();
    Jdbc.inTransaction(API, client -> {
      Friendships.of(this).delete();
      client.sql(Jdbc.sql("api/delete_photo_likes")).params(id, id).update();
      client.sql(Jdbc.sql("api/delete_likes")).param(id).update();
      client.sql(Jdbc.sql("api/delete_photos")).param(id).update();
      client.sql(Jdbc.sql("api/delete_statistics")).param(id).update();
      client.sql(Jdbc.sql("api/delete_user")).param(id).update();
    });
  }

  @Override
  public boolean exists() {
    return Jdbc.client(API).sql(Jdbc.sql("api/exists_user")).param(id.toString())
        .query(Boolean.class).single();
  }
}
