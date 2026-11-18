package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import org.junit.jupiter.api.Test;

@WebTest
public class IncomeInvitationsTabTest {

  @Test
  void shouldShowOnlyIncomeInvitations(
      @User(incomeInvitations = 1, outcomeInvitations = 1) TestUser user, Page page) {
    People.open(user, page)
        .clickIncomeInvitationsTab()
        .checkPeople(user.incomeInvitations().getFirst().username());
  }

  @Test
  void shouldOfferAcceptAndDeclineForInviter(@User(incomeInvitations = 1) TestUser user,
      Page page) {
    People.open(user, page)
        .clickIncomeInvitationsTab()
        .checkActions(user.incomeInvitations().getFirst().username(), "Accept", "Decline");
  }

  @Test
  void shouldShowEmptyStateWithoutIncomeInvitations(@User TestUser user, Page page) {
    People.open(user, page)
        .clickIncomeInvitationsTab()
        .checkNoPeople();
  }
}
