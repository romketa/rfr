package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migration;
import io.student.rangiffler.data.db.SchemaHistory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class AllureEnvironmentExtension implements SuiteExtension {

  public static final String RESULTS_PARAMETER = "allure.results.directory";
  public static final String FILE_NAME = "environment.properties";
  public static final List<String> KEYS = List.of("frontUrl", "authUrl", "apiUrl", "browser",
      "headless", "commit", "authSchemaVersion", "apiSchemaVersion");

  private static final String DEFAULT_RESULTS = "rfr-e2e/build/allure-results";
  private static final String BROWSER = "chromium";

  @Override
  public void afterSuite(ExtensionContext context) {
    Path results = context.getConfigurationParameter(RESULTS_PARAMETER).map(Path::of)
        .orElseGet(() -> Config.repoRoot().resolve(DEFAULT_RESULTS));
    List<String> values = List.of(Config.frontUrl(), Config.authUrl(), Config.apiUrl(), BROWSER,
        String.valueOf(Config.headless()), Config.commit(), version(Databases.AUTH),
        version(Databases.API));
    try {
      Files.createDirectories(results);
      Files.write(results.resolve(FILE_NAME), lines(values), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Allure environment could not be written to " + results.resolve(FILE_NAME), e);
    }
  }

  private static List<String> lines(List<String> values) {
    return KEYS.stream().map(key -> key + "=" + values.get(KEYS.indexOf(key))).toList();
  }

  private static String version(Databases db) {
    try {
      List<Migration> applied = SchemaHistory.of(db).applied();
      return applied.isEmpty() ? "none" : applied.getLast().version();
    } catch (DataAccessException e) {
      return "unavailable";
    }
  }
}
