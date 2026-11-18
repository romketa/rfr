package io.student.rangiffler.test.people;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.Test;

@WebTest
public class FriendsTabTest {

  @Test
  void shouldShowOnlyFriends(@User(friends = 1, incomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    People.open(user, landingPage)
        .table()
        .checkPeople(user.friends().getFirst().username());
  }

  @Test
  void shouldOfferRemoveForFriend(@User(friends = 1) TestUser user, LandingPage landingPage) {
    People.open(user, landingPage)
        .table()
        .row()
        .checkActions(user.friends().getFirst().username(), "Remove");
  }

  @Test
  void shouldShowEmptyStateWithoutFriends(@User TestUser user, LandingPage landingPage) {
    People.open(user, landingPage)
        .table()
        .checkNoPeople();
  }
}
