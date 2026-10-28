package io.student.rangiffler.util;

import java.security.SecureRandom;
import java.util.stream.Collectors;

/**
 * Generated values satisfy the constraints of {@code RegistrationForm} in rfr-auth:
 * username 3–50 chars, password 3–12 chars, no whitespace in either.
 */
public final class DataUtils {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
  private static final String ALPHANUMERIC = LOWER + LOWER.toUpperCase() + "0123456789";
  private static final String PASSWORD_ALPHABET = ALPHANUMERIC + "!@#$%^&*";

  private DataUtils() {
  }

  public static String randomUsername() {
    return "user_" + randomString(LOWER + "0123456789", 8);
  }

  public static String randomPassword() {
    return randomString(PASSWORD_ALPHABET, 12);
  }

  private static String randomString(String alphabet, int length) {
    return RANDOM.ints(length, 0, alphabet.length())
        .mapToObj(alphabet::charAt)
        .map(String::valueOf)
        .collect(Collectors.joining());
  }
}
