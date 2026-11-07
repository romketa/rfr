package io.student.rangiffler.data.db;

import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.API;

public final class Friendship implements Row {

  private final UUID id;
  private final Profile requester;
  private final Profile addressee;
  private final FriendshipStatus status;

  private Friendship(UUID id, Profile requester, Profile addressee, FriendshipStatus status) {
    this.id = id;
    this.requester = requester;
    this.addressee = addressee;
    this.status = status;
  }

  public static Friendship accepted(Profile requester, Profile addressee) {
    return new Friendship(UUID.randomUUID(), requester, addressee, FriendshipStatus.ACCEPTED);
  }

  public static Friendship pending(Profile requester, Profile addressee) {
    return new Friendship(UUID.randomUUID(), requester, addressee, FriendshipStatus.PENDING);
  }

  @Override
  public Friendship create() {
    String sql = switch (status) {
      case ACCEPTED -> "api/insert_friendship";
      case PENDING -> "api/insert_invitation";
    };
    Jdbc.client(API).sql(Jdbc.sql(sql))
        .params(id.toString(), requester.id().toString(), addressee.id().toString())
        .update();
    return this;
  }

  @Override
  public void delete() {
    Jdbc.client(API).sql(Jdbc.sql("api/delete_friendship")).param(id.toString()).update();
  }

  @Override
  public boolean exists() {
    return Jdbc.client(API).sql(Jdbc.sql("api/exists_friendship")).param(id.toString())
        .query(Boolean.class).single();
  }
}
