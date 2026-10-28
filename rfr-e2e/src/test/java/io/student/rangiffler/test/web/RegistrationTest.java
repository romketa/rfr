package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.util.DataUtils;
import org.junit.jupiter.api.Test;

@WebTest
public class RegistrationTest {

  @Test
  void shouldAuthorizeAfterRegistration(Page page) {
    String username = DataUtils.randomUsername();
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
}
