package io.student.rangiffler.data.db;

import java.util.List;

public final class SchemaHistory {

  private final Databases db;
  private final String sql;

  private SchemaHistory(Databases db, String sql) {
    this.db = db;
    this.sql = sql;
  }

  public static SchemaHistory of(Databases db) {
    return new SchemaHistory(db, switch (db) {
      case AUTH -> "auth/select_migrations";
      case API -> "api/select_migrations";
    });
  }

  public List<Migration> applied() {
    return Jdbc.client(db).sql(Jdbc.sql(sql))
        .query((rs, rowNum) -> new Migration(rs.getString("version"), rs.getString("description"),
            rs.getInt("checksum")))
        .list();
  }
}
