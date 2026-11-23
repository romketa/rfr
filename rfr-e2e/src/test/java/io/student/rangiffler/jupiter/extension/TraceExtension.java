package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opentest4j.TestAbortedException;

public class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  private static final Path TRACES = Path.of("build", "traces");

  @Override
  public void beforeEach(ExtensionContext context) {
    BrowserContextExtension.getOrCreateBrowserContext(context).tracing().start(
        new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
  }

  @Override
  public void afterEach(ExtensionContext context) throws IOException {
    Tracing tracing = BrowserContextExtension.getOrCreateBrowserContext(context).tracing();
    if (context.getExecutionException()
        .filter(e -> !(e instanceof TestAbortedException))
        .isEmpty()) {
      tracing.stop();
      return;
    }
    Path trace = TRACES.resolve(context.getRequiredTestClass().getName())
        .resolve(context.getRequiredTestMethod().getName())
        .resolve("trace.zip");
    Files.createDirectories(trace.getParent());
    tracing.stop(new Tracing.StopOptions().setPath(trace));
    try (InputStream content = Files.newInputStream(trace)) {
      Allure.attachment("trace.zip", "application/zip", content,
          AttachmentOptions.withFileExtension("zip"));
    }
  }
}
