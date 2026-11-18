package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StandReadinessExtension implements SuiteExtension {

  public static final String UI_TAG = "ui";
  public static final String CHECK_PARAMETER = "rfr.stand.check";
  public static final String TIMEOUT_PARAMETER = "rfr.stand.timeout";
  public static final String MYSQL_PARAMETER = "rfr.stand.mysql";
  public static final String AUTH_PARAMETER = "rfr.stand.auth";
  public static final String API_PARAMETER = "rfr.stand.api";
  public static final String FRONT_PARAMETER = "rfr.stand.front";

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
  private static final String OIDC_DISCOVERY = "/.well-known/openid-configuration";
  private static final String GRAPHQL = "/graphql";
  private static final String TYPENAME_QUERY = "{\"query\":\"{__typename}\"}";
  private static final Pattern STATUS_LINE = Pattern.compile("HTTP/\\S+ (\\d{3})( .*)?");
  private static final int FIRST_ERROR_STATUS = 400;

  private record Service(String name, String address, BiPredicate<String, Instant> probe,
                         String start) {

    boolean answers(Instant deadline) {
      return probe.test(address, deadline);
    }

    @Override
    public String toString() {
      return name + " at " + address;
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (context.getTags().contains(UI_TAG) && enabled(context)) {
      SuiteExtension.super.beforeAll(context);
    }
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    Duration timeout = timeout(context);
    Instant deadline = Instant.now().plus(timeout);
    List<Service> pending = new ArrayList<>(services(context));
    while (true) {
      pending.removeIf(service -> service.answers(deadline));
      if (pending.isEmpty()) {
        return;
      }
      if (!Instant.now().isBefore(deadline)) {
        throw new IllegalStateException("The stand is not ready after " + timeout.toSeconds()
            + " s: " + pending.stream().map(Service::toString).collect(Collectors.joining(", "))
            + " did not answer; start with " + pending.stream().map(Service::start).distinct()
            .collect(Collectors.joining(" and ")));
      }
      pause(deadline);
    }
  }

  private static List<Service> services(ExtensionContext context) {
    return List.of(
        new Service("mysql", parameter(context, MYSQL_PARAMETER, mysqlAddress()),
            StandReadinessExtension::acceptsTcp, "docker compose up -d --wait"),
        new Service("rfr-auth", parameter(context, AUTH_PARAMETER, Config.authUrl()),
            (url, deadline) -> statuses("GET", url + OIDC_DISCOVERY, "", deadline)
                .anyMatch(status -> status < FIRST_ERROR_STATUS),
            "./gradlew runAll"),
        new Service("rfr-api", parameter(context, API_PARAMETER, Config.apiUrl()),
            (url, deadline) -> statuses("POST", url + GRAPHQL, TYPENAME_QUERY, deadline)
                .findAny().isPresent(),
            "./gradlew runAll"),
        new Service("frontend", parameter(context, FRONT_PARAMETER, Config.frontUrl()),
            (url, deadline) -> statuses("GET", url, "", deadline)
                .anyMatch(status -> status < FIRST_ERROR_STATUS),
            "npm run dev in rfr-gql-client"));
  }

  private static boolean enabled(ExtensionContext context) {
    return context.getConfigurationParameter(CHECK_PARAMETER).map(Boolean::parseBoolean)
        .orElseGet(Config::standCheck);
  }

  private static Duration timeout(ExtensionContext context) {
    return context.getConfigurationParameter(TIMEOUT_PARAMETER).map(Long::parseLong)
        .map(Duration::ofSeconds).orElseGet(Config::standTimeout);
  }

  private static String parameter(ExtensionContext context, String name, String fallback) {
    return context.getConfigurationParameter(name).orElse(fallback);
  }

  private static String mysqlAddress() {
    URI url = URI.create(Config.authDbUrl().substring("jdbc:".length()));
    return url.getHost() + ":" + url.getPort();
  }

  private static boolean acceptsTcp(String address, Instant deadline) {
    String[] hostAndPort = address.split(":");
    int port = Integer.parseInt(hostAndPort[1]);
    return addresses(hostAndPort[0]).stream().anyMatch(ip -> {
      try (Socket socket = connect(ip, port, deadline)) {
        return true;
      } catch (IOException e) {
        return false;
      }
    });
  }

  private static Stream<Integer> statuses(String method, String url, String body,
      Instant deadline) {
    URI uri = URI.create(url);
    int port = uri.getPort() == -1 ? 80 : uri.getPort();
    byte[] content = body.getBytes(StandardCharsets.UTF_8);
    String head = method + " " + (uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
        + " HTTP/1.1\r\nHost: " + uri.getHost() + ":" + port
        + "\r\nConnection: close\r\nContent-Type: application/json\r\nContent-Length: "
        + content.length + "\r\n\r\n";
    return addresses(uri.getHost()).stream().flatMap(ip -> {
      try (Socket socket = connect(ip, port, deadline)) {
        socket.getOutputStream().write(head.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().write(content);
        socket.getOutputStream().flush();
        return Optional.ofNullable(new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine())
            .map(STATUS_LINE::matcher).filter(Matcher::matches)
            .map(line -> Integer.parseInt(line.group(1))).stream();
      } catch (IOException e) {
        return Stream.empty();
      }
    });
  }

  private static List<InetAddress> addresses(String host) {
    try {
      return List.of(InetAddress.getAllByName(host));
    } catch (UnknownHostException e) {
      return List.of();
    }
  }

  private static Socket connect(InetAddress ip, int port, Instant deadline) throws IOException {
    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(ip, port), attemptMillis(deadline));
      socket.setSoTimeout(attemptMillis(deadline));
      return socket;
    } catch (IOException e) {
      socket.close();
      throw e;
    }
  }

  private static int attemptMillis(Instant deadline) throws SocketTimeoutException {
    long left = Duration.between(Instant.now(), deadline).toMillis();
    if (left <= 0) {
      throw new SocketTimeoutException("The stand timeout expired before the request");
    }
    return (int) Math.min(left, REQUEST_TIMEOUT.toMillis());
  }

  private static void pause(Instant deadline) {
    Duration left = Duration.between(Instant.now(), deadline);
    if (left.isNegative() || left.isZero()) {
      return;
    }
    try {
      Thread.sleep(left.compareTo(POLL_INTERVAL) < 0 ? left : POLL_INTERVAL);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the stand", e);
    }
  }
}
