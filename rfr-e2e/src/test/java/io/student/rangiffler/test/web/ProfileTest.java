package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.ProfilePage;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Every saved value is checked after {@link ProfilePage#reload()}: the Apollo cache shows the
 * mutation result immediately, so only a fresh page load proves that rfr-api persisted it.
 */
@WebTest
public class ProfileTest {

  private static final Path AVATAR = classpathFile("img/avatar.png");
  // A new user gets this country from rfr-api on the first login
  private static final String DEFAULT_LOCATION = "Russian Federation";

  @Test
  void shouldSaveFirstname(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillFirstname("Ivan")
        .clickSave()
        .checkSuccessMessage()
        .reload()
        .checkFirstname("Ivan");
  }

  @Test
  void shouldSaveSurname(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillSurname("Petrov")
        .clickSave()
        .checkSuccessMessage()
        .reload()
        .checkSurname("Petrov");
  }

  @Test
  void shouldSaveLocation(@User TestUser user, Page page) {
    openProfile(user, page)
        .selectLocation("Canada")
        .clickSave()
        .checkSuccessMessage()
        .reload()
        .checkLocation("Canada");
  }

  @Test
  void shouldSaveAvatar(@User TestUser user, Page page) {
    openProfile(user, page)
        .uploadAvatar(AVATAR)
        .clickSave()
        .checkSuccessMessage()
        .reload()
        .checkAvatar(AVATAR);
  }

  @Test
  void shouldShowProfileCreatedBeforeFirstLogin(
      @User(firstname = "Ivan", surname = "Petrov", country = "ca") TestUser user, Page page) {
    openProfile(user, page)
        .checkFirstname("Ivan")
        .checkSurname("Petrov")
        .checkLocation("Canada");
  }

  @Test
  void shouldDiscardUnsavedChangesOnReset(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillFirstname("Ivan")
        .fillSurname("Petrov")
        .selectLocation("Canada")
        .uploadAvatar(AVATAR)
        .clickReset()
        .checkFirstname("")
        .checkSurname("")
        .checkLocation(DEFAULT_LOCATION)
        .checkNoAvatar();
  }

  @Test
  void shouldResetToSavedProfile(@User TestUser user, Page page) {
    openProfile(user, page)
        .fillFirstname("Ivan")
        .selectLocation("Canada")
        .clickSave()
        .checkSuccessMessage()
        .fillFirstname("Fedor")
        .selectLocation("France")
        .clickReset()
        .checkFirstname("Ivan")
        .checkLocation("Canada");
  }

  private static ProfilePage openProfile(TestUser user, Page page) {
    return new LandingPage(page)
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn()
        .header()
        .clickProfile();
  }

  private static Path classpathFile(String name) {
    try {
      return Path.of(Objects.requireNonNull(ProfileTest.class.getClassLoader().getResource(name),
          "Resource " + name + " is missing from the test classpath").toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Resource " + name + " has an invalid URI", e);
    }
  }
}
