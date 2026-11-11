package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.Config;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
  private static final String HTTP_PREFIX = "HTTP/";

  private record Service(String name, String address, Predicate<String> probe, String start) {

    boolean answers() {
      return probe.test(address);
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
      pending.removeIf(Service::answers);
      if (pending.isEmpty()) {
        return;
      }
      if (Instant.now().isAfter(deadline)) {
        throw new IllegalStateException("The stand is not ready after " + timeout.toSeconds()
            + " s: " + pending.stream().map(Service::toString).collect(Collectors.joining(", "))
            + " did not answer; start with " + pending.stream().map(Service::start).distinct()
            .collect(Collectors.joining(" and ")));
      }
      pause();
    }
  }

  private static List<Service> services(ExtensionContext context) {
    return List.of(
        new Service("mysql", parameter(context, MYSQL_PARAMETER, mysqlAddress()),
            StandReadinessExtension::acceptsTcp, "docker compose up -d --wait"),
        new Service("rfr-auth", parameter(context, AUTH_PARAMETER, Config.authUrl()),
            StandReadinessExtension::answersHttp, "./gradlew runAll"),
        new Service("rfr-api", parameter(context, API_PARAMETER, Config.apiUrl()),
            StandReadinessExtension::answersHttp, "./gradlew runAll"),
        new Service("frontend", parameter(context, FRONT_PARAMETER, Config.frontUrl()),
            StandReadinessExtension::answersHttp, "npm run dev in rfr-gql-client"));
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

  private static boolean acceptsTcp(String address) {
    String[] hostAndPort = address.split(":");
    int port = Integer.parseInt(hostAndPort[1]);
    return addresses(hostAndPort[0]).stream().anyMatch(ip -> {
      try (Socket socket = connect(ip, port)) {
        return true;
      } catch (IOException e) {
        return false;
      }
    });
  }

  private static boolean answersHttp(String url) {
    URI uri = URI.create(url);
    int port = uri.getPort() == -1 ? 80 : uri.getPort();
    return addresses(uri.getHost()).stream().anyMatch(ip -> {
      try (Socket socket = connect(ip, port)) {
        socket.getOutputStream().write(("GET / HTTP/1.1\r\nHost: " + uri.getHost() + ":" + port
            + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
        byte[] head = new byte[HTTP_PREFIX.length()];
        int read = socket.getInputStream().readNBytes(head, 0, head.length);
        return new String(head, 0, read, StandardCharsets.US_ASCII).equals(HTTP_PREFIX);
      } catch (IOException e) {
        return false;
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

  private static Socket connect(InetAddress ip, int port) throws IOException {
    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(ip, port), (int) REQUEST_TIMEOUT.toMillis());
      socket.setSoTimeout((int) REQUEST_TIMEOUT.toMillis());
      return socket;
    } catch (IOException e) {
      socket.close();
      throw e;
    }
  }

  private static void pause() {
    try {
      Thread.sleep(POLL_INTERVAL);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the stand", e);
    }
  }
}
