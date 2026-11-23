package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import io.student.rangiffler.page.ExamplePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Template of a reusable page fragment. A page holds it as a field and exposes it through a method,
 * so the same header, dialog or table row is written once and composed into every page that shows it.
 */
@Singleton
public final class ExampleComponent {

  private final Provider<ExamplePage> examplePage;
  private final Locator logoutButton;

  @Inject
  public ExampleComponent(Page page, Provider<ExamplePage> examplePage) {
    this.examplePage = examplePage;
    // Scope every locator to the component root: the same control often repeats elsewhere on the page
    Locator root = page.getByRole(AriaRole.BANNER);
    this.logoutButton = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout"));
  }

  @Step("Click Logout")
  public ExamplePage clickLogout() {
    logoutButton.click();
    return examplePage.get();
  }

  @Step("Check that header is shown for an authorized user")
  public ExampleComponent checkAuthorized() {
    assertThat(logoutButton).isVisible();
    return this;
  }
}
