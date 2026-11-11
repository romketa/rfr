package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import org.junit.jupiter.api.Test;

@WebTest
public class OutcomeInvitationsTabTest {

  @Test
  void shouldShowOnlyOutcomeInvitations(
      @User(outcomeInvitations = 1, incomeInvitations = 1) TestUser user, Page page) {
    People.open(user, page)
        .clickOutcomeInvitationsTab()
        .checkPeople(user.outcomeInvitations().getFirst().username());
  }

  @Test
  void shouldOfferWaitingAndCancelForInvitee(@User(outcomeInvitations = 1) TestUser user,
      Page page) {
    String invitee = user.outcomeInvitations().getFirst().username();
    People.open(user, page)
        .clickOutcomeInvitationsTab()
        .checkWaiting(invitee)
        .checkActions(invitee, "Cancel");
  }

  @Test
  void shouldShowEmptyStateWithoutOutcomeInvitations(@User TestUser user, Page page) {
    People.open(user, page)
        .clickOutcomeInvitationsTab()
        .checkNoPeople();
  }
}
