package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

public final class LandingPage {

  public static final String URL = Config.frontUrl() + "/";

  private final Page page;
  private final Locator loginButton;
  private final Locator registerLink;

  public LandingPage(Page page) {
    this.page = page;
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
    return new RegisterPage(page);
  }

  @Step("Click Login")
  public LoginPage clickLogin() {
    loginButton.click();
    return new LoginPage(page);
  }
}
