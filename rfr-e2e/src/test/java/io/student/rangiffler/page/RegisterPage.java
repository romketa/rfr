package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegisterPage {

  public static final String URL = Config.authUrl() + "/register";

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator passwordConfirmInput;
  private final Locator submitButton;
  private final Locator successMessage;
  private final Locator signInLink;

  public RegisterPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username"));
    // The Thymeleaf labels wrap the "Show password" buttons, so both password fields lack a clean accessible name
    this.passwordInput = page.locator("#password");
    this.passwordConfirmInput = page.locator("#passwordSubmit");
    this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up"));
    this.successMessage = page.getByText("Congratulations! You've registered!");
    this.signInLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in"));
  }

  @Step("Open registration page")
  public RegisterPage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Fill registration form for {username}")
  public RegisterPage fillRegistrationForm(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    passwordConfirmInput.fill(password);
    return this;
  }

  @Step("Click Sign up")
  public RegisterPage clickSignUp() {
    submitButton.click();
    return this;
  }

  @Step("Click Sign in")
  public LandingPage clickSignIn() {
    signInLink.click();
    return new LandingPage(page);
  }

  @Step("Check that registration succeeded")
  public RegisterPage checkSuccessMessage() {
    assertThat(successMessage).hasText("Congratulations! You've registered!");
    return this;
  }
}
