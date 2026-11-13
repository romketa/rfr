package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.Config;

public final class BrowserOptions implements OptionsFactory {

  public static final String TEST_ID_ATTRIBUTE = "data-test-id";

  @Override
  public Options getOptions() {
    return new Options()
        .setHeadless(Config.headless())
        .setTestIdAttribute(TEST_ID_ATTRIBUTE)
        .setContextOptions(contextOptions());
  }

  private static Browser.NewContextOptions contextOptions() {
    return new Browser.NewContextOptions()
        .setViewportSize(Config.viewportWidth(), Config.viewportHeight())
        .setLocale(Config.browserLocale());
  }
}
