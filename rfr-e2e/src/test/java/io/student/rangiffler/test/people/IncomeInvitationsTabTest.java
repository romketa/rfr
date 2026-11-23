package io.student.rangiffler.test.people;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.Test;

@WebTest
public class IncomeInvitationsTabTest {

  @Test
  void shouldShowOnlyIncomeInvitations(
      @User(incomeInvitations = 1, outcomeInvitations = 1) TestUser user, LandingPage landingPage) {
    People.open(user, landingPage)
        .clickIncomeInvitationsTab()
        .table()
        .checkPeople(user.incomeInvitations().getFirst().username());
  }

  @Test
  void shouldOfferAcceptAndDeclineForInviter(@User(incomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    People.open(user, landingPage)
        .clickIncomeInvitationsTab()
        .table()
        .row()
        .checkActions(user.incomeInvitations().getFirst().username(), "Accept", "Decline");
  }

  @Test
  void shouldShowEmptyStateWithoutIncomeInvitations(@User TestUser user, LandingPage landingPage) {
    People.open(user, landingPage)
        .clickIncomeInvitationsTab()
        .table()
        .checkNoPeople();
  }
}
