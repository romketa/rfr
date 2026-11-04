package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.util.DataUtils;
import org.junit.jupiter.api.Test;

@WebTest
public class LoginNegativeTest {

  @Test
  void shouldRejectWrongPassword(@User TestUser user, Page page) {
    new LandingPage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password() + "-wrong")
        .clickSignInExpectingError()
        .checkBadCredentialsError();
  }

  @Test
  void shouldRejectUnknownUser(Page page) {
    new LandingPage(page)
        .open()
        .clickLogin()
        .fillLoginForm(DataUtils.randomUsername(), DataUtils.randomPassword())
        .clickSignInExpectingError()
        .checkBadCredentialsError();
  }
}
