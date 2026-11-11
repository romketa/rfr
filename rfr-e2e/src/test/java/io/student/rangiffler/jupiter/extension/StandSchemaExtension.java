package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migration;
import io.student.rangiffler.data.db.SchemaDrift;
import io.student.rangiffler.data.db.SchemaHistory;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StandSchemaExtension implements SuiteExtension {

  public static final String ROOT_PARAMETER = "rfr.migrations.root";

  private static final String RECREATE_DATABASE =
      "docker compose down && docker volume rm mysql-volume && docker volume create mysql-volume"
          + " && docker compose up -d";

  private record Schema(Databases db, String module, String name) {

    Path migrations(Path root) {
      return root.resolve(module).resolve("src/main/resources/db/migration").resolve(name);
    }
  }

  private static final List<Schema> SCHEMAS = List.of(
      new Schema(Databases.AUTH, "rfr-auth", "rangiffler-auth"),
      new Schema(Databases.API, "rfr-api", "rangiffler-api"));

  @Override
  public void beforeSuite(ExtensionContext context) {
    Path root = context.getConfigurationParameter(ROOT_PARAMETER).map(Path::of)
        .orElseGet(Config::repoRoot);
    String report = SCHEMAS.stream()
        .map(schema -> report(schema, root))
        .filter(lines -> !lines.isEmpty())
        .collect(Collectors.joining(System.lineSeparator()));
    if (!report.isEmpty()) {
      throw new IllegalStateException(
          "The local database does not match the backend migrations checked out in " + root
              + System.lineSeparator() + report);
    }
  }

  private static String report(Schema schema, Path root) {
    Path migrations = schema.migrations(root);
    SchemaDrift drift = SchemaDrift.between(SchemaHistory.of(schema.db()).applied(),
        Migration.inDirectory(migrations));
    if (drift.isEmpty()) {
      return "";
    }
    return Stream.of(
            line(drift.ahead(), "applied to " + schema.name() + " but missing from " + migrations,
                "check out the backend branch that has them, or recreate the database with "
                    + RECREATE_DATABASE + " and restart rfr-auth and rfr-api"),
            line(drift.behind(), "present in " + migrations + " but not applied to " + schema.name(),
                "restart " + schema.module() + " so that Flyway applies them"),
            line(drift.changed(), "changed in " + migrations + " since " + schema.name()
                    + " was migrated",
                "recreate the database with " + RECREATE_DATABASE
                    + " and restart rfr-auth and rfr-api"))
        .filter(line -> !line.isEmpty())
        .collect(Collectors.joining(System.lineSeparator()));
  }

  private static String line(List<Migration> migrations, String state, String action) {
    return migrations.isEmpty() ? "" : "  " + migrations + " " + state + ": " + action;
  }
}
