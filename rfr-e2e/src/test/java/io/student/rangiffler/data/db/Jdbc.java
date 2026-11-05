package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Entry point for the DAOs: a {@link JdbcClient} per database, transactions that hand the client to the callback so
 * the action cannot pick a client of another database, and SQL loaded from {@code sql/<name>.sql} on the classpath.
 */
public final class Jdbc {

  private static final Map<Databases, JdbcClient> CLIENTS = new ConcurrentHashMap<>();
  private static final Map<String, String> SQL = new ConcurrentHashMap<>();

  private Jdbc() {
  }

  public static JdbcClient client(Databases db) {
    return CLIENTS.computeIfAbsent(db, d -> JdbcClient.create(d.dataSource()));
  }

  public static void inTransaction(Databases db, Consumer<JdbcClient> action) {
    inTransactionReturning(db, client -> {
      action.accept(client);
      return null;
    });
  }

  // A distinct name, not an overload: javac cannot tell Consumer from Function for an implicitly typed lambda
  public static <T> T inTransactionReturning(Databases db, Function<JdbcClient, T> action) {
    return new TransactionTemplate(db.transactionManager()).execute(status -> action.apply(client(db)));
  }

  /**
   * Returns the statement stored in {@code sql/<name>.sql}; the name carries the schema folder, e.g. {@code auth/insert_user}.
   */
  public static String sql(String name) {
    return SQL.computeIfAbsent(name, Jdbc::read);
  }

  private static String read(String name) {
    String path = "sql/" + name + ".sql";
    try (InputStream in = Jdbc.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("SQL resource " + path + " is missing from the test classpath");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("SQL resource " + path + " could not be read", e);
    }
  }
}
