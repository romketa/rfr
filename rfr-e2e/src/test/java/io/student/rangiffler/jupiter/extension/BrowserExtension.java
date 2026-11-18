package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import com.microsoft.playwright.impl.junit.PageExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.Config;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.ByteArrayInputStream;

public class BrowserExtension implements BeforeEachCallback, AfterEachCallback {

  static {
    PlaywrightAssertions.setDefaultAssertionTimeout(Config.actionTimeoutMs());
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    BrowserContext browserContext = BrowserContextExtension.getOrCreateBrowserContext(context);
    browserContext.setDefaultTimeout(Config.actionTimeoutMs());
    browserContext.setDefaultNavigationTimeout(Config.navigationTimeoutMs());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (context.getExecutionException().isPresent()) {
      Allure.attachment("Screenshot on failure", "image/png",
          new ByteArrayInputStream(PageExtension.getOrCreatePage(context)
              .screenshot(new Page.ScreenshotOptions().setFullPage(true))),
          AttachmentOptions.withFileExtension("png"));
    }
  }
}
