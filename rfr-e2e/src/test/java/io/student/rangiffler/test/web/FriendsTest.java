package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.MyTravelsPage;
import io.student.rangiffler.page.PeoplePage;
import org.junit.jupiter.api.Test;

import java.util.List;

@WebTest
public class FriendsTest {

  @Test
  void shouldShowFriends(@User(friends = 2) TestUser user, Page page) {
    openPeople(user, page)
        .checkPeople(usernames(user.friends()));
  }

  @Test
  void shouldShowIncomeInvitations(@User(incomeInvitations = 2) TestUser user, Page page) {
    openPeople(user, page)
        .clickIncomeInvitationsTab()
        .checkPeople(usernames(user.incomeInvitations()));
  }

  @Test
  void shouldShowOutcomeInvitations(@User(outcomeInvitations = 2) TestUser user, Page page) {
    openPeople(user, page)
        .clickOutcomeInvitationsTab()
        .checkPeople(usernames(user.outcomeInvitations()));
  }

  @Test
  void shouldShowIncomeInvitationToInvitee(@User(outcomeInvitations = 1) TestUser user, Page page) {
    openPeople(user.outcomeInvitations().getFirst(), page)
        .clickIncomeInvitationsTab()
        .checkPeople(user.username());
  }

  @Test
  void shouldCancelOutcomeInvitation(@User(outcomeInvitations = 1) TestUser user, Page page) {
    openPeople(user, page)
        .clickOutcomeInvitationsTab()
        .clickCancel(user.outcomeInvitations().getFirst().username())
        .checkMessage("Invitation cancelled")
        .reload()
        .clickOutcomeInvitationsTab()
        .checkPeople();
  }

  @Test
  void shouldBecomeFriendsAfterInviteeAccepts(@User(outcomeInvitations = 1) TestUser user,
      Page page) {
    TestUser invitee = user.outcomeInvitations().getFirst();
    PeoplePage inviteePeople = openPeople(invitee, page)
        .clickIncomeInvitationsTab()
        .clickAccept(user.username())
        .checkMessage("Invitation accepted");
    login(inviteePeople.header().clickLogout(), user)
        .sidebar()
        .clickPeople()
        .checkPeople(invitee.username());
  }

  @Test
  void shouldShowOutcomeInvitationToInviter(@User(incomeInvitations = 1) TestUser user, Page page) {
    openPeople(user.incomeInvitations().getFirst(), page)
        .clickOutcomeInvitationsTab()
        .checkPeople(user.username());
  }

  @Test
  void shouldAcceptIncomeInvitation(@User(incomeInvitations = 1) TestUser user, Page page) {
    TestUser inviter = user.incomeInvitations().getFirst();
    openPeople(user, page)
        .clickIncomeInvitationsTab()
        .clickAccept(inviter.username())
        .checkMessage("Invitation accepted")
        .reload()
        .checkPeople(inviter.username());
  }

  @Test
  void shouldSendInvitationInBrowser(@User TestUser sender,
      @User(firstname = "Receiver") TestUser receiver,
      Page page) {
    sendInvitation(sender, receiver, page);
  }

  @Test
  void shouldAcceptInvitationSentInBrowser(@User TestUser sender,
      @User(firstname = "Receiver") TestUser receiver,
      Page page) {
    login(sendInvitation(sender, receiver, page).header().clickLogout(), receiver)
        .sidebar()
        .clickPeople()
        .clickIncomeInvitationsTab()
        .clickAccept(sender.username())
        .checkMessage("Invitation accepted")
        .reload()
        .checkPeople(sender.username());
  }

  private static PeoplePage sendInvitation(TestUser sender, TestUser receiver, Page page) {
    return openPeople(sender, page)
        .clickAllPeopleTab()
        .fillSearch(receiver.username())
        .clickSearch()
        .clickAdd(receiver.username())
        .checkMessage("Invitation sent")
        .clickOutcomeInvitationsTab()
        .checkPeople(receiver.username());
  }

  private static PeoplePage openPeople(TestUser user, Page page) {
    return login(new LandingPage(page).open(), user)
        .sidebar()
        .clickPeople();
  }

  private static MyTravelsPage login(LandingPage landing, TestUser user) {
    return landing
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }

  private static String[] usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toArray(String[]::new);
  }
}
