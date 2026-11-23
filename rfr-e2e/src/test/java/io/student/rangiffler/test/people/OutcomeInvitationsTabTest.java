package io.student.rangiffler.test.people;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.Test;

@WebTest
public class OutcomeInvitationsTabTest {

  @Test
  void shouldShowOnlyOutcomeInvitations(
      @User(outcomeInvitations = 1, incomeInvitations = 1) TestUser user, LandingPage landingPage) {
    People.open(user, landingPage)
        .clickOutcomeInvitationsTab()
        .table()
        .checkPeople(user.outcomeInvitations().getFirst().username());
  }

  @Test
  void shouldOfferWaitingAndCancelForInvitee(@User(outcomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    String invitee = user.outcomeInvitations().getFirst().username();
    People.open(user, landingPage)
        .clickOutcomeInvitationsTab()
        .table()
        .row()
        .checkWaiting(invitee)
        .checkActions(invitee, "Cancel");
  }

  @Test
  void shouldShowEmptyStateWithoutOutcomeInvitations(@User TestUser user, LandingPage landingPage) {
    People.open(user, landingPage)
        .clickOutcomeInvitationsTab()
        .table()
        .checkNoPeople();
  }
}
