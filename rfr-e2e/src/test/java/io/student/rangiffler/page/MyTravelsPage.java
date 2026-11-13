package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.page.component.Header;
import io.student.rangiffler.page.component.Sidebar;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {

  public static final String URL = Config.frontUrl() + "/my-travels";

  private final Page page;
  private final Header header;
  private final Sidebar sidebar;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.sidebar = new Sidebar(page);
  }

  @Step("Open my travels page")
  public MyTravelsPage open() {
    page.navigate(URL);
    return this;
  }

  public Header header() {
    return header;
  }

  public Sidebar sidebar() {
    return sidebar;
  }

  @Step("Check that the user is authorized")
  public MyTravelsPage checkAuthorized() {
    assertThat(page).hasURL(Pattern.compile("/my-travels$"));
    header.checkAuthorized();
    return this;
  }
}
