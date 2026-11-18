package io.student.rangiffler.test.people;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeoplePage;

final class People {

  private People() {
  }

  static PeoplePage open(TestUser user, LandingPage landingPage) {
    return landingPage
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .sidebar()
        .clickPeople();
  }
}
