package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import org.junit.jupiter.api.Test;

@WebTest
public class FriendsTabTest {

  @Test
  void shouldShowOnlyFriends(@User(friends = 1, incomeInvitations = 1) TestUser user, Page page) {
    People.open(user, page)
        .checkPeople(user.friends().getFirst().username());
  }

  @Test
  void shouldOfferRemoveForFriend(@User(friends = 1) TestUser user, Page page) {
    People.open(user, page)
        .checkActions(user.friends().getFirst().username(), "Remove");
  }

  @Test
  void shouldShowEmptyStateWithoutFriends(@User TestUser user, Page page) {
    People.open(user, page)
        .checkNoPeople();
  }
}
