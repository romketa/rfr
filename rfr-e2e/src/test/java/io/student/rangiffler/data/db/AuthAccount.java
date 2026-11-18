package io.student.rangiffler.data.db;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.AUTH;

public final class AuthAccount implements Row {

  private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

  private final UUID id;
  private final String username;
  private final String passwordHash;

  private AuthAccount(UUID id, String username, String passwordHash) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
  }

  public static AuthAccount random(String username, String password) {
    return new AuthAccount(UUID.randomUUID(), username, PASSWORD_ENCODER.encode(password));
  }

  public static Optional<AuthAccount> find(String username) {
    return Jdbc.client(AUTH).sql(Jdbc.sql("auth/select_user")).param(username)
        .query((rs, rowNum) -> new AuthAccount(UUID.fromString(rs.getString("id")), username,
            rs.getString("password")))
        .optional();
  }

  public UUID id() {
    return id;
  }

  @Override
  public AuthAccount create() {
    String id = this.id.toString();
    Jdbc.inTransaction(AUTH, client -> {
      client.sql(Jdbc.sql("auth/insert_user")).params(id, username, passwordHash).update();
      client.sql(Jdbc.sql("auth/insert_authorities")).params(id, id).update();
    });
    return this;
  }

  @Override
  public void delete() {
    String id = this.id.toString();
    Jdbc.inTransaction(AUTH, client -> {
      client.sql(Jdbc.sql("auth/delete_authorities")).param(id).update();
      client.sql(Jdbc.sql("auth/delete_user")).param(id).update();
    });
  }

  @Override
  public boolean exists() {
    return Jdbc.client(AUTH).sql(Jdbc.sql("auth/exists_user")).param(id.toString())
        .query(Boolean.class).single();
  }
}
