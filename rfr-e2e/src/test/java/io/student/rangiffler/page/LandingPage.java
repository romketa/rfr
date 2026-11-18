package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import io.student.rangiffler.config.Config;

@Singleton
public final class LandingPage {

  public static final String URL = Config.frontUrl() + "/";

  private final Page page;
  private final Locator loginButton;
  private final Locator registerLink;
  private final Provider<RegisterPage> registerPage;
  private final Provider<LoginPage> loginPage;

  @Inject
  public LandingPage(Page page, Provider<RegisterPage> registerPage,
      Provider<LoginPage> loginPage) {
    this.page = page;
    this.registerPage = registerPage;
    this.loginPage = loginPage;
    this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
    this.registerLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register"));
  }

  @Step("Open landing page")
  public LandingPage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Click Register")
  public RegisterPage clickRegister() {
    registerLink.click();
    return registerPage.get();
  }

  @Step("Click Login")
  public LoginPage clickLogin() {
    loginButton.click();
    return loginPage.get();
  }
}
