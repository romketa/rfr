package io.student.rangiffler.data.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.API;

public final class Friendship implements Row {

  private final UUID id;
  private final UUID requesterId;
  private final UUID addresseeId;
  private final FriendshipStatus status;

  private Friendship(UUID id, UUID requesterId, UUID addresseeId, FriendshipStatus status) {
    this.id = id;
    this.requesterId = requesterId;
    this.addresseeId = addresseeId;
    this.status = status;
  }

  public static Friendship accepted(Profile requester, Profile addressee) {
    return new Friendship(UUID.randomUUID(), requester.id(), addressee.id(),
        FriendshipStatus.ACCEPTED);
  }

  public static Friendship pending(Profile requester, Profile addressee) {
    return new Friendship(UUID.randomUUID(), requester.id(), addressee.id(),
        FriendshipStatus.PENDING);
  }

  public static Optional<Friendship> find(UUID id) {
    return Jdbc.client(API).sql(Jdbc.sql("api/select_friendship")).param(id.toString())
        .query(Friendship::fromRow).optional();
  }

  public static Optional<Friendship> find(Profile requester, Profile addressee) {
    return Jdbc.client(API).sql(Jdbc.sql("api/select_friendship_between"))
        .params(requester.id().toString(), addressee.id().toString())
        .query(Friendship::fromRow).optional();
  }

  private static Friendship fromRow(ResultSet rs, int rowNum) throws SQLException {
    return new Friendship(UUID.fromString(rs.getString("id")),
        UUID.fromString(rs.getString("requester_id")),
        UUID.fromString(rs.getString("addressee_id")),
        FriendshipStatus.valueOf(rs.getString("status")));
  }

  public UUID id() {
    return id;
  }

  public FriendshipStatus status() {
    return status;
  }

  @Override
  public Friendship create() {
    String sql = switch (status) {
      case ACCEPTED -> "api/insert_friendship";
      case PENDING -> "api/insert_invitation";
    };
    Jdbc.client(API).sql(Jdbc.sql(sql))
        .params(id.toString(), requesterId.toString(), addresseeId.toString())
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
