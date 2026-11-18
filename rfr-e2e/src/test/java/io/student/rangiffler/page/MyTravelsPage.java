package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.page.component.Header;
import io.student.rangiffler.page.component.PhotoCard;
import io.student.rangiffler.page.component.Sidebar;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {

  public static final String URL = Config.frontUrl() + "/my-travels";

  private final Page page;
  private final Header header;
  private final Sidebar sidebar;
  private final Locator photoCards;
  private final Locator visitedCountriesButton;
  private final Locator visitedCountriesList;
  private final Locator countryFilter;
  private final Locator resetCountryFilterButton;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.sidebar = new Sidebar(page);
    this.photoCards = page.getByRole(AriaRole.ARTICLE);
    this.visitedCountriesButton = page.getByRole(AriaRole.BUTTON)
        .filter(new Locator.FilterOptions().setHasText(Pattern.compile("countr(y|ies) visited$")));
    this.visitedCountriesList = page.getByRole(AriaRole.TOOLTIP);
    this.countryFilter = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName(Pattern.compile("^Country filter: ")));
    this.resetCountryFilterButton = countryFilter.getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Reset country filter").setExact(true));
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

  public PhotoCard photo(String description) {
    return new PhotoCard(page, photoCards, description);
  }

  @Step("Click the visited countries counter")
  public MyTravelsPage clickVisitedCountries() {
    visitedCountriesButton.click();
    return this;
  }

  @Step("Select country {country} in the visited countries list")
  public MyTravelsPage selectCountry(String country) {
    visitedCountriesList.getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(country).setExact(true)).click();
    return this;
  }

  @Step("Click Reset country filter")
  public MyTravelsPage clickResetCountryFilter() {
    resetCountryFilterButton.click();
    return this;
  }

  @Step("Check that the user is authorized")
  public MyTravelsPage checkAuthorized() {
    assertThat(page).hasURL(Pattern.compile("/my-travels$"));
    header.checkAuthorized();
    return this;
  }

  @Step("Check that {count} photos are shown")
  public MyTravelsPage checkPhotosCount(int count) {
    assertThat(photoCards).hasCount(count);
    return this;
  }

  @Step("Check that {count} countries are visited")
  public MyTravelsPage checkVisitedCountries(int count) {
    assertThat(visitedCountriesButton)
        .hasText(count + (count == 1 ? " country visited" : " countries visited"));
    return this;
  }

  @Step("Check that photos are filtered by country {country}")
  public MyTravelsPage checkCountryFilter(String country) {
    assertThat(countryFilter).hasAccessibleName("Country filter: " + country);
    return this;
  }

  @Step("Check that photos are not filtered by country")
  public MyTravelsPage checkNoCountryFilter() {
    assertThat(countryFilter).isHidden();
    return this;
  }
}
