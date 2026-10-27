package io.student.rangiffler.config;

import java.util.Locale;

/**
 * Resolution order for every key: system property {@code rfr.<key>}, then environment variable
 * {@code RFR_<KEY>} (upper case, dots replaced with underscores), then the constant below.
 */
public final class Config {

  private static final String FRONT_URL = "http://localhost:3001";
  private static final String AUTH_URL = "http://localhost:9001";
  private static final boolean HEADLESS = true;
  private static final double ACTION_TIMEOUT_MS = 10_000;
  private static final String AUTH_DB_URL = "jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC";
  private static final String API_DB_URL = "jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC";
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "secret";

  private Config() {
  }

  public static String frontUrl() {
    return resolve("front.url", FRONT_URL);
  }

  public static String authUrl() {
    return resolve("auth.url", AUTH_URL);
  }

  public static boolean headless() {
    return Boolean.parseBoolean(resolve("headless", String.valueOf(HEADLESS)));
  }

  public static double actionTimeoutMs() {
    return Double.parseDouble(resolve("timeout.ms", String.valueOf(ACTION_TIMEOUT_MS)));
  }

  public static String authDbUrl() {
    return resolve("auth.db.url", AUTH_DB_URL);
  }

  public static String apiDbUrl() {
    return resolve("api.db.url", API_DB_URL);
  }

  public static String dbUser() {
    return resolve("db.user", DB_USER);
  }

  public static String dbPassword() {
    return resolve("db.password", DB_PASSWORD);
  }

  private static String resolve(String key, String fallback) {
    String property = System.getProperty("rfr." + key);
    if (property != null) {
      return property;
    }
    String env = System.getenv("RFR_" + key.toUpperCase(Locale.ROOT).replace('.', '_'));
    return env != null ? env : fallback;
  }
}
