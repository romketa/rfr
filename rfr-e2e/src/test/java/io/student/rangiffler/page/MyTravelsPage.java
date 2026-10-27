package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.page.component.Header;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class MyTravelsPage {

  public static final String URL = Config.frontUrl() + "/my-travels";

  private final Page page;
  private final Header header;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new Header(page);
  }

  @Step("Open my travels page")
  public MyTravelsPage open() {
    page.navigate(URL);
    return this;
  }

  public Header header() {
    return header;
  }

  /**
   * The Apollo cache renders the page before the token round-trip is proven, so the access token
   * in localStorage is checked explicitly: it is what every GraphQL request carries.
   */
  @Step("Check that the user is authorized")
  public MyTravelsPage checkAuthorized() {
    assertThat(page).hasURL(Pattern.compile("/my-travels$"));
    header.checkAuthorized();
    // Playwright has no assertion for localStorage, hence the plain JUnit assertion
    String accessToken = (String) page.evaluate("() => window.localStorage.getItem('access_token') ?? ''");
    assertFalse(accessToken.isBlank(), "Access token is missing in localStorage after login");
    return this;
  }
}
