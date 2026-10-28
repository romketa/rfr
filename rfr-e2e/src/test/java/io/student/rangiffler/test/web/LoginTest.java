package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.Test;

@WebTest
public class LoginTest {

  @Test
  @User
  void shouldAuthorizeExistingUser(TestUser user, Page page) {
    new LandingPage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .checkAuthorized();
  }
}
