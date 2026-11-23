package io.student.rangiffler.test.web;

import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.Test;

@WebTest
public class LoginTest {

  @Test
  void shouldAuthorizeExistingUser(@User TestUser user, LandingPage landingPage) {
    landingPage
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .checkAuthorized();
  }
}
