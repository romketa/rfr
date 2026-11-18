package io.student.rangiffler.jupiter.probe;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
  private static final long SILENT_TIMEOUT_SECONDS = 1;
  private static final Duration SILENT_WAIT_LIMIT = Duration.ofSeconds(3);
  private static final int UNAUTHORIZED = 401;
  private static final int UNAVAILABLE = 503;

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
    assertTrue(!api.requests().isEmpty(), "the late service was never asked");
  }

  @Test
  void shouldFailBeforeTestsWhenServiceNeverAnswers() throws IOException {
    api.close();
    api = FakeHttp.reserved();

    EngineExecutionResults results = run(UiTarget.class, SHORT_TIMEOUT_SECONDS, true);

    results.testEvents().assertStatistics(stats -> stats.started(0));
    Throwable failure = failure(results);
    assertInstanceOf(IllegalStateException.class, failure);
    String message = failure.getMessage();
    assertAll(
        () -> assertTrue(message.contains("rfr-api at " + api.url()), message),
        () -> assertTrue(message.contains("./gradlew runAll"), message),
        () -> assertTrue(message.contains("after " + SHORT_TIMEOUT_SECONDS + " s"), message));
  }

  @Test
  void shouldAskEveryServiceForItsOwnSignOfReadiness() {
    EngineExecutionResults results = run(UiTarget.class, POLL_TIMEOUT_SECONDS, true);

    results.testEvents().assertStatistics(stats -> stats.succeeded(1));
    assertAll(
        () -> assertEquals(1, mysql.connections(), "mysql"),
        () -> assertEquals(List.of("GET /.well-known/openid-configuration"), auth.requests()),
        () -> assertEquals(List.of("POST /graphql {\"query\":\"{__typename}\"}"), api.requests()),
        () -> assertEquals(List.of("GET /"), front.requests()));
  }

  @Test
  void shouldNotTakeAuthAnsweringWithErrorForReady() throws IOException {
    auth.close();
    auth = FakeHttp.answering(UNAVAILABLE);

    EngineExecutionResults results = run(UiTarget.class, SHORT_TIMEOUT_SECONDS, true);

    results.testEvents().assertStatistics(stats -> stats.started(0));
    String message = failure(results).getMessage();
    assertTrue(message.contains("rfr-auth at " + auth.url()), message);
  }

  @Test
  void shouldTakeApiRejectingAnonymousQueryForReady() throws IOException {
    api.close();
    api = FakeHttp.answering(UNAUTHORIZED);

    run(UiTarget.class, POLL_TIMEOUT_SECONDS, true).testEvents()
        .assertStatistics(stats -> stats.succeeded(1));
  }

  @Test
  void shouldNotWaitLongerThanTimeoutWhenServicesAcceptButNeverAnswer() throws IOException {
    auth.close();
    api.close();
    front.close();
    auth = FakeHttp.silent();
    api = FakeHttp.silent();
    front = FakeHttp.silent();
    Instant start = Instant.now();

    EngineExecutionResults results = run(UiTarget.class, SILENT_TIMEOUT_SECONDS, true);

    Duration waited = Duration.between(start, Instant.now());
    results.testEvents().assertStatistics(stats -> stats.started(0));
    assertTrue(waited.compareTo(SILENT_WAIT_LIMIT) < 0,
        "waited " + waited.toMillis() + " ms with a timeout of " + SILENT_TIMEOUT_SECONDS + " s");
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

  private static Throwable failure(EngineExecutionResults results) {
    return results.containerEvents().failed().stream()
        .findFirst()
        .flatMap(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable())
        .orElseThrow(() -> new AssertionError("No container failed although the stand is down"));
  }

  private void assertNoRequests() {
    assertAll(
        () -> assertEquals(0, mysql.connections(), "mysql"),
        () -> assertEquals(List.of(), auth.requests(), "rfr-auth"),
        () -> assertEquals(List.of(), api.requests(), "rfr-api"),
        () -> assertEquals(List.of(), front.requests(), "frontend"));
  }

  private static final class FakeHttp {

    private static final int OK = 200;

    private final int port;
    private final int status;
    private final Queue<String> requests = new ConcurrentLinkedQueue<>();
    private HttpServer server;
    private ServerSocket silent;

    private FakeHttp(int port, int status) {
      this.port = port;
      this.status = status;
    }

    static FakeHttp listening() throws IOException {
      return answering(OK);
    }

    static FakeHttp answering(int status) throws IOException {
      FakeHttp fake = reserved(status);
      fake.start();
      return fake;
    }

    static FakeHttp reserved() throws IOException {
      return reserved(OK);
    }

    static FakeHttp silent() throws IOException {
      ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
      FakeHttp fake = new FakeHttp(socket.getLocalPort(), OK);
      fake.silent = socket;
      return fake;
    }

    private static FakeHttp reserved(int status) throws IOException {
      try (ServerSocket socket = new ServerSocket(0)) {
        return new FakeHttp(socket.getLocalPort(), status);
      }
    }

    void start() {
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      server.createContext("/", exchange -> {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add((exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
            + body).strip());
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
      });
      server.start();
    }

    String url() {
      return "http://127.0.0.1:" + port;
    }

    List<String> requests() {
      return List.copyOf(requests);
    }

    void close() throws IOException {
      if (server != null) {
        server.stop(0);
        server = null;
      }
      if (silent != null) {
        silent.close();
        silent = null;
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
  @ExtendWith(StandReadinessExtension.class)
  static final class UiTarget {

    @Test
    void shouldRunOnlyWhenTheStandAnswers() {
    }
  }

  @ProbeTarget
  @ExtendWith(StandReadinessExtension.class)
  static final class PlainTarget {

    @Test
    void shouldRunWithoutTheStand() {
    }
  }
}
