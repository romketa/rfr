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
    Playwright playwright = Playwright.create();
    context.getStore(NAMESPACE).put(Playwright.class, playwright);
    Browser browser = playwright.chromium()
        .launch(new BrowserType.LaunchOptions().setHeadless(Config.headless()));
    Page page = browser.newContext(
        new Browser.NewContextOptions().setLocale(Config.browserLocale())).newPage();
    page.setDefaultTimeout(Config.actionTimeoutMs());
    page.setDefaultNavigationTimeout(Config.actionTimeoutMs());
    context.getStore(NAMESPACE).put(Page.class, page);
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
