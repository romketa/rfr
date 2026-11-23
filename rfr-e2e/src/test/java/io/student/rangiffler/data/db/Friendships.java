package io.student.rangiffler.data.db;

import static io.student.rangiffler.data.db.Databases.API;

public final class Friendships implements Rows {

  private final Profile owner;
  private final String deleteSql;
  private final String existsSql;

  private Friendships(Profile owner, String deleteSql, String existsSql) {
    this.owner = owner;
    this.deleteSql = deleteSql;
    this.existsSql = existsSql;
  }

  public static Friendships of(Profile owner) {
    return new Friendships(owner, "api/delete_friendships", "api/exists_friendships");
  }

  public static Friendships of(Profile owner, FriendshipStatus status) {
    return switch (status) {
      case ACCEPTED -> new Friendships(owner, "api/delete_friends", "api/exists_friends");
      case PENDING -> new Friendships(owner, "api/delete_invitations", "api/exists_invitations");
    };
  }

  @Override
  public void delete() {
    String id = owner.id().toString();
    Jdbc.client(API).sql(Jdbc.sql(deleteSql)).params(id, id).update();
  }

  @Override
  public boolean exists() {
    String id = owner.id().toString();
    return Jdbc.client(API).sql(Jdbc.sql(existsSql)).params(id, id).query(Boolean.class).single();
  }
}
