package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.UserRecords;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.util.DataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@WebTest
public class RegistrationTest {

  // A field, not a local: JUnit creates a test instance per method, and @AfterEach needs the name the test registered
  private final String username = DataUtils.randomUsername();

  @Test
  void shouldAuthorizeAfterRegistration(Page page) {
    String password = DataUtils.randomPassword();

    new LandingPage(page)
        .open()
        .clickRegister()
        .fillRegistrationForm(username, password)
        .clickSignUp()
        .checkSuccessMessage()
        .clickSignIn()
        .clickLogin()
        .fillLoginForm(username, password)
        .clickSignIn()
        .checkAuthorized();
  }

  /**
   * The user is created through the UI, out of reach of any fixture, so it is removed here; @AfterEach also runs
   * when the scenario fails after the registration, and deleting a user that was never created is a no-op.
   */
  @AfterEach
  void deleteRegisteredUser() {
    UserRecords.delete(username);
  }
}
