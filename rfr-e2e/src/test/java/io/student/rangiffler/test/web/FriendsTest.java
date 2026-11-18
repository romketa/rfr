package io.student.rangiffler.test.web;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.MyTravelsPage;
import io.student.rangiffler.page.PeoplePage;
import org.junit.jupiter.api.Test;

@WebTest
public class FriendsTest {

  @Test
  void shouldShowIncomeInvitationToInvitee(@User(outcomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    openPeople(user.outcomeInvitations().getFirst(), landingPage)
        .clickIncomeInvitationsTab()
        .table()
        .checkPeople(user.username());
  }

  @Test
  void shouldCancelOutcomeInvitation(@User(outcomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    openPeople(user, landingPage)
        .clickOutcomeInvitationsTab()
        .table()
        .row()
        .clickCancel(user.outcomeInvitations().getFirst().username())
        .checkMessage("Invitation cancelled")
        .reload()
        .clickOutcomeInvitationsTab()
        .table()
        .checkPeople();
  }

  @Test
  void shouldBecomeFriendsAfterInviteeAccepts(@User(outcomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    TestUser invitee = user.outcomeInvitations().getFirst();
    PeoplePage inviteePeople = openPeople(invitee, landingPage)
        .clickIncomeInvitationsTab()
        .table()
        .row()
        .clickAccept(user.username())
        .checkMessage("Invitation accepted");
    login(inviteePeople.header().clickLogout(), user)
        .sidebar()
        .clickPeople()
        .table()
        .checkPeople(invitee.username());
  }

  @Test
  void shouldShowOutcomeInvitationToInviter(@User(incomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    openPeople(user.incomeInvitations().getFirst(), landingPage)
        .clickOutcomeInvitationsTab()
        .table()
        .checkPeople(user.username());
  }

  @Test
  void shouldAcceptIncomeInvitation(@User(incomeInvitations = 1) TestUser user,
      LandingPage landingPage) {
    TestUser inviter = user.incomeInvitations().getFirst();
    openPeople(user, landingPage)
        .clickIncomeInvitationsTab()
        .table()
        .row()
        .clickAccept(inviter.username())
        .checkMessage("Invitation accepted")
        .reload()
        .table()
        .checkPeople(inviter.username());
  }

  @Test
  void shouldSendInvitationInBrowser(@User TestUser sender,
      @User(firstname = "Receiver") TestUser receiver,
      LandingPage landingPage) {
    sendInvitation(sender, receiver, landingPage);
  }

  @Test
  void shouldAcceptInvitationSentInBrowser(@User TestUser sender,
      @User(firstname = "Receiver") TestUser receiver,
      LandingPage landingPage) {
    login(sendInvitation(sender, receiver, landingPage).header().clickLogout(), receiver)
        .sidebar()
        .clickPeople()
        .clickIncomeInvitationsTab()
        .table()
        .row()
        .clickAccept(sender.username())
        .checkMessage("Invitation accepted")
        .reload()
        .table()
        .checkPeople(sender.username());
  }

  private static PeoplePage sendInvitation(TestUser sender, TestUser receiver,
      LandingPage landingPage) {
    PeoplePage people = openPeople(sender, landingPage)
        .clickAllPeopleTab()
        .fillSearch(receiver.username())
        .clickSearch()
        .table()
        .row()
        .clickAdd(receiver.username())
        .checkMessage("Invitation sent")
        .clickOutcomeInvitationsTab();
    people.table().checkPeople(receiver.username());
    return people;
  }

  private static PeoplePage openPeople(TestUser user, LandingPage landingPage) {
    return login(landingPage.open(), user)
        .sidebar()
        .clickPeople();
  }

  private static MyTravelsPage login(LandingPage landing, TestUser user) {
    return landing
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }
}
