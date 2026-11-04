package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage {

  public static final String URL = Config.authUrl() + "/login";

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator submitButton;
  private final Locator errorMessage;

  public LoginPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username"));
    // The Thymeleaf label wraps the "Show password" button, so the field has no clean accessible name
    this.passwordInput = page.locator("input[name='password']");
    this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
    // The Thymeleaf error paragraph has no role or label
    this.errorMessage = page.locator(".form__error");
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

  /**
   * Rejected credentials keep the user on the login page, so the chain continues here instead of on MyTravelsPage.
   */
  @Step("Click Sign in expecting an error")
  public LoginPage clickSignInExpectingError() {
    submitButton.click();
    return this;
  }

  /**
   * Spring Security reports a wrong password and an unknown username with the same message, so the page does not
   * reveal which usernames exist.
   */
  @Step("Check that sign in is rejected with 'Bad credentials'")
  public LoginPage checkBadCredentialsError() {
    assertThat(page).hasURL(Pattern.compile("/login\\?error$"));
    assertThat(errorMessage).hasText("Bad credentials");
    return this;
  }
}
