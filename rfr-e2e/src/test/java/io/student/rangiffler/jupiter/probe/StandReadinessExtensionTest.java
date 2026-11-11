package io.student.rangiffler.jupiter.probe;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class StandReadinessExtensionTest {

  private static final Duration START_DELAY = Duration.ofSeconds(2);
  private static final long POLL_TIMEOUT_SECONDS = 10;
  private static final long SHORT_TIMEOUT_SECONDS = 3;

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private FakeMysql mysql;
  private FakeHttp auth;
  private FakeHttp api;
  private FakeHttp front;

  @BeforeEach
  void startFakeStand() throws IOException {
    mysql = new FakeMysql();
    auth = FakeHttp.listening();
    api = FakeHttp.listening();
    front = FakeHttp.listening();
  }

  @AfterEach
  void stopFakeStand() throws IOException {
    scheduler.shutdownNow();
    mysql.close();
    auth.close();
    api.close();
    front.close();
  }

  @Test
  void shouldRunTestsWhenServiceStartsAnsweringWithinTimeout() throws IOException {
    api.close();
    api = FakeHttp.reserved();
    scheduler.schedule(api::start, START_DELAY.toMillis(), TimeUnit.MILLISECONDS);

    EngineExecutionResults results = run(UiTarget.class, POLL_TIMEOUT_SECONDS, true);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    assertTrue(api.requests() > 0, "the late service was never asked");
  }

  @Test
  void shouldFailBeforeTestsWhenServiceNeverAnswers() throws IOException {
    api.close();
    api = FakeHttp.reserved();

    EngineExecutionResults results = run(UiTarget.class, SHORT_TIMEOUT_SECONDS, true);

    results.testEvents().assertStatistics(stats -> stats.started(0));
    Throwable failure = results.containerEvents().failed().stream()
        .findFirst()
        .flatMap(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable())
        .orElseThrow(() -> new AssertionError("No container failed although rfr-api is down"));
    assertInstanceOf(IllegalStateException.class, failure);
    String message = failure.getMessage();
    assertAll(
        () -> assertTrue(message.contains("rfr-api at " + api.url()), message),
        () -> assertTrue(message.contains("./gradlew runAll"), message),
        () -> assertTrue(message.contains("after " + SHORT_TIMEOUT_SECONDS + " s"), message));
  }

  @Test
  void shouldNotTouchStandForClassWithoutUiTag() {
    EngineExecutionResults results = run(PlainTarget.class, POLL_TIMEOUT_SECONDS, true);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    assertNoRequests();
  }

  @Test
  void shouldNotTouchStandWhenCheckIsDisabled() {
    EngineExecutionResults results = run(UiTarget.class, POLL_TIMEOUT_SECONDS, false);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    assertNoRequests();
  }

  private EngineExecutionResults run(Class<?> target, long timeoutSeconds, boolean check) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true")
        .configurationParameters(Map.of(
            StandReadinessExtension.CHECK_PARAMETER, String.valueOf(check),
            StandReadinessExtension.TIMEOUT_PARAMETER, String.valueOf(timeoutSeconds),
            StandReadinessExtension.MYSQL_PARAMETER, mysql.address(),
            StandReadinessExtension.AUTH_PARAMETER, auth.url(),
            StandReadinessExtension.API_PARAMETER, api.url(),
            StandReadinessExtension.FRONT_PARAMETER, front.url()))
        .selectors(selectClass(target))
        .execute();
  }

  private void assertNoRequests() {
    assertAll(
        () -> assertEquals(0, mysql.connections(), "mysql"),
        () -> assertEquals(0, auth.requests(), "rfr-auth"),
        () -> assertEquals(0, api.requests(), "rfr-api"),
        () -> assertEquals(0, front.requests(), "frontend"));
  }

  private static final class FakeHttp {

    private final int port;
    private final AtomicInteger requests = new AtomicInteger();
    private HttpServer server;

    private FakeHttp(int port) {
      this.port = port;
    }

    static FakeHttp listening() throws IOException {
      FakeHttp fake = reserved();
      fake.start();
      return fake;
    }

    static FakeHttp reserved() throws IOException {
      try (ServerSocket socket = new ServerSocket(0)) {
        return new FakeHttp(socket.getLocalPort());
      }
    }

    void start() {
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      server.createContext("/", exchange -> {
        requests.incrementAndGet();
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
      });
      server.start();
    }

    String url() {
      return "http://127.0.0.1:" + port;
    }

    int requests() {
      return requests.get();
    }

    void close() {
      if (server != null) {
        server.stop(0);
        server = null;
      }
    }
  }

  private static final class FakeMysql implements AutoCloseable {

    private final ServerSocket socket = new ServerSocket(0, 50, null);
    private final AtomicInteger connections = new AtomicInteger();
    private final CountDownLatch stopped = new CountDownLatch(1);

    FakeMysql() throws IOException {
      Thread.ofVirtual().start(() -> {
        while (!socket.isClosed()) {
          try (Socket client = socket.accept()) {
            connections.incrementAndGet();
          } catch (IOException e) {
            break;
          }
        }
        stopped.countDown();
      });
    }

    String address() {
      return "127.0.0.1:" + socket.getLocalPort();
    }

    int connections() {
      return connections.get();
    }

    @Override
    public void close() throws IOException {
      socket.close();
      try {
        assertTrue(stopped.await(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "fake mysql did not stop");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @ProbeTarget
  @Tag(StandReadinessExtension.UI_TAG)
  static final class UiTarget {

    @Test
    void shouldRunOnlyWhenTheStandAnswers() {
    }
  }

  @ProbeTarget
  static final class PlainTarget {

    @Test
    void shouldRunWithoutTheStand() {
    }
  }
}
