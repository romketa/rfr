package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

public final class LoginPage {

  public static final String URL = Config.authUrl() + "/login";

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator submitButton;

  public LoginPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username"));
    // The Thymeleaf label wraps the "Show password" button, so the field has no clean accessible name
    this.passwordInput = page.locator("input[name='password']");
    this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
  }

  @Step("Open login page")
  public LoginPage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Fill login form for {username}")
  public LoginPage fillLoginForm(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    return this;
  }

  @Step("Click Sign in")
  public MyTravelsPage clickSignIn() {
    submitButton.click();
    return new MyTravelsPage(page);
  }
}
