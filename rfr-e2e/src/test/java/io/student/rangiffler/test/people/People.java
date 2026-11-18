package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeoplePage;

final class People {

  private People() {
  }

  static PeoplePage open(TestUser user, Page page) {
    return new LandingPage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .sidebar()
        .clickPeople();
  }
}
