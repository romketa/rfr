package io.student.rangiffler.test.web;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.util.DataUtils;
import org.junit.jupiter.api.Test;

@WebTest
public class LoginNegativeTest {

  @Test
  void shouldRejectWrongPassword(@User TestUser user, LandingPage landingPage) {
    landingPage
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password() + "-wrong")
        .clickSignInExpectingError()
        .checkBadCredentialsError();
  }

  @Test
  void shouldRejectUnknownUser(LandingPage landingPage) {
    landingPage
        .open()
        .clickLogin()
        .fillLoginForm(DataUtils.randomUsername(), DataUtils.randomPassword())
        .clickSignInExpectingError()
        .checkBadCredentialsError();
  }
}
