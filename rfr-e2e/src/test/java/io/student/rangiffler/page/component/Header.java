package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.ProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Application bar shown on every private page: logo link, profile avatar and logout.
 */
@Singleton
public final class Header {

  private final Provider<LandingPage> landingPage;
  private final Provider<ProfilePage> profilePage;
  private final Locator logoutButton;
  private final Locator profileLink;

  @Inject
  public Header(Page page, Provider<LandingPage> landingPage,
      Provider<ProfilePage> profilePage) {
    this.landingPage = landingPage;
    this.profilePage = profilePage;
    // The side menu repeats the Profile link, so every locator is scoped to the app bar
    Locator root = page.getByRole(AriaRole.BANNER);
    this.logoutButton = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout"));
    this.profileLink = root.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile"));
  }

  @Step("Click Logout")
  public LandingPage clickLogout() {
    logoutButton.click();
    return landingPage.get();
  }

  @Step("Click Profile")
  public ProfilePage clickProfile() {
    profileLink.click();
    return profilePage.get();
  }

  @Step("Check that header is shown for an authorized user")
  public Header checkAuthorized() {
    assertThat(logoutButton).isVisible();
    assertThat(profileLink).isVisible();
    return this;
  }
}
