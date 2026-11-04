package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Profile form: avatar, first name, surname, location, Save and Reset.
 * Reset restores the values of the signed-in user known to the frontend, not empty fields.
 */
public final class ProfilePage {

  public static final String URL = Config.frontUrl() + "/profile";

  private final Page page;
  private final Locator firstnameInput;
  private final Locator surnameInput;
  private final Locator locationSelect;
  private final Locator avatarInput;
  private final Locator avatarImage;
  private final Locator saveButton;
  private final Locator resetButton;
  private final Locator snackbar;

  public ProfilePage(Page page) {
    this.page = page;
    Locator form = page.locator("form").filter(new Locator.FilterOptions()
        .setHas(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("My profile"))));
    this.firstnameInput = form.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("First Name"));
    this.surnameInput = form.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("Surname"));
    this.locationSelect = form.getByRole(AriaRole.COMBOBOX, new Locator.GetByRoleOptions().setName("Location"));
    // The file input is hidden behind the "Update avatar" label and has no accessible name
    this.avatarInput = form.locator("input[type='file']");
    // The avatar image has no alt, and the selected country flag is an image in the same form, so the MUI class is
    // the only stable hook
    this.avatarImage = form.locator("img.MuiAvatar-img");
    this.saveButton = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save"));
    this.resetButton = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset"));
    this.snackbar = page.getByRole(AriaRole.ALERT);
  }

  @Step("Open profile page")
  public ProfilePage open() {
    page.navigate(URL);
    return this;
  }

  /**
   * A fresh document drops the Apollo cache, so the form is filled from a new GetUser request: this is how a test
   * proves that a value reached the server and not only the cache.
   */
  @Step("Reload profile page")
  public ProfilePage reload() {
    page.reload();
    return this;
  }

  @Step("Fill first name with {firstname}")
  public ProfilePage fillFirstname(String firstname) {
    firstnameInput.fill(firstname);
    return this;
  }

  @Step("Fill surname with {surname}")
  public ProfilePage fillSurname(String surname) {
    surnameInput.fill(surname);
    return this;
  }

  @Step("Select location {country}")
  public ProfilePage selectLocation(String country) {
    locationSelect.click();
    // The option name also contains the flag alt text, so it is matched as a substring
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(country)).click();
    return this;
  }

  @Step("Upload avatar {image}")
  public ProfilePage uploadAvatar(Path image) {
    avatarInput.setInputFiles(image);
    return this;
  }

  @Step("Click Save")
  public ProfilePage clickSave() {
    saveButton.click();
    return this;
  }

  @Step("Click Reset")
  public ProfilePage clickReset() {
    resetButton.click();
    return this;
  }

  @Step("Check that the profile is saved")
  public ProfilePage checkSuccessMessage() {
    assertThat(snackbar).hasText("Your profile is successfully updated");
    return this;
  }

  @Step("Check that first name is '{firstname}'")
  public ProfilePage checkFirstname(String firstname) {
    assertThat(firstnameInput).hasValue(firstname);
    return this;
  }

  @Step("Check that surname is '{surname}'")
  public ProfilePage checkSurname(String surname) {
    assertThat(surnameInput).hasValue(surname);
    return this;
  }

  @Step("Check that location is {country}")
  public ProfilePage checkLocation(String country) {
    assertThat(locationSelect).hasText(country);
    return this;
  }

  /**
   * The frontend reads the file with FileReader.readAsDataURL and sends the data URL as is, so the server must
   * return exactly the data URL of the uploaded file.
   */
  @Step("Check that avatar is {image}")
  public ProfilePage checkAvatar(Path image) {
    assertThat(avatarImage).hasAttribute("src", dataUrl(image));
    return this;
  }

  @Step("Check that avatar is not set")
  public ProfilePage checkNoAvatar() {
    assertThat(avatarImage).hasCount(0);
    return this;
  }

  private static String dataUrl(Path image) {
    String mimeType = URLConnection.guessContentTypeFromName(image.getFileName().toString());
    if (mimeType == null) {
      throw new IllegalArgumentException("Cannot guess the MIME type of avatar " + image);
    }
    try {
      return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(image));
    } catch (IOException e) {
      throw new UncheckedIOException("Avatar " + image + " cannot be read", e);
    }
  }
}
