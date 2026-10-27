package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.Config;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.ByteArrayInputStream;

/**
 * Opens a fresh browser per test so tests can run in parallel without sharing cookies or localStorage,
 * injects {@link Page} into test methods and attaches a screenshot to Allure when a test fails.
 */
public class BrowserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BrowserExtension.class);

  static {
    // Page timeouts do not apply to PlaywrightAssertions, they have their own default of 5 seconds.
    // Set once at class load, outside any test, so the Allure aspect does not report it as a test step
    PlaywrightAssertions.setDefaultAssertionTimeout(Config.actionTimeoutMs());
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    Playwright playwright = Playwright.create();
    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(Config.headless()));
    Page page = browser.newContext().newPage();
    page.setDefaultTimeout(Config.actionTimeoutMs());
    page.setDefaultNavigationTimeout(Config.actionTimeoutMs());
    // Both values are AutoCloseable, so the store closes them itself in reverse insertion order: page first, then playwright
    context.getStore(NAMESPACE).put(Playwright.class, playwright);
    context.getStore(NAMESPACE).put(Page.class, page);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    Page page = context.getStore(NAMESPACE).get(Page.class, Page.class);
    if (context.getExecutionException().isPresent()) {
      Allure.attachment("Screenshot on failure", "image/png",
          new ByteArrayInputStream(page.screenshot(new Page.ScreenshotOptions().setFullPage(true))),
          AttachmentOptions.withFileExtension("png"));
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == Page.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(Page.class, Page.class);
  }
}
