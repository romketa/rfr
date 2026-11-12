package io.student.rangiffler.data;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class UsersQueue {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static final List<TestUser> POOL = List.of(
      staticUser("rfr_static_1", "static1!"),
      staticUser("rfr_static_2", "static2!"),
      staticUser("rfr_static_3", "static3!"));
  private static final BlockingQueue<TestUser> FREE = new LinkedBlockingQueue<>(POOL);

  private UsersQueue() {
  }

  static TestUser take() {
    try {
      TestUser user = FREE.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      if (user == null) {
        throw new IllegalStateException(
            "None of " + POOL.size() + " static users became free within "
                + TIMEOUT.toSeconds() + " s, every one is still held by a running test");
      }
      return user;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for a static user", e);
    }
  }

  static void release(TestUser user) {
    if (!POOL.contains(user)) {
      throw new IllegalArgumentException(
          "User " + user.username() + " does not belong to the static pool");
    }
    FREE.add(user);
  }

  public static int available() {
    return FREE.size();
  }

  private static TestUser staticUser(String username, String password) {
    return new TestUser(UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)),
        username, password, List.of(), List.of(), List.of());
  }
}
