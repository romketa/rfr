package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.LandingPage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Application bar shown on every private page: logo link, profile avatar and logout.
 */
public final class Header {

  private final Page page;
  private final Locator logoutButton;
  private final Locator profileLink;

  public Header(Page page) {
    this.page = page;
    // The side menu repeats the Profile link, so every locator is scoped to the app bar
    Locator root = page.getByRole(AriaRole.BANNER);
    this.logoutButton = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout"));
    this.profileLink = root.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile"));
  }

  @Step("Click Logout")
  public LandingPage clickLogout() {
    logoutButton.click();
    return new LandingPage(page);
  }

  @Step("Check that header is shown for an authorized user")
  public Header checkAuthorized() {
    assertThat(logoutButton).isVisible();
    assertThat(profileLink).isVisible();
    return this;
  }
}
