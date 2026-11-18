package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.page.component.Header;
import io.student.rangiffler.page.component.PeopleTable;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Singleton
public final class PeoplePage {

  public static final String URL = Config.frontUrl() + "/people";

  private final Page page;
  private final Header header;
  private final PeopleTable table;
  private final Locator allPeopleTab;
  private final Locator incomeInvitationsTab;
  private final Locator outcomeInvitationsTab;
  private final Locator searchInput;
  private final Locator searchButton;
  private final Locator snackbar;

  @Inject
  public PeoplePage(Page page, Header header, PeopleTable table) {
    this.page = page;
    this.header = header;
    this.table = table;
    this.allPeopleTab = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("All People"));
    this.incomeInvitationsTab = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Incoming invitations"));
    this.outcomeInvitationsTab =
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Outcome invitations"));
    this.searchInput = page.getByRole(AriaRole.TEXTBOX,
        new Page.GetByRoleOptions().setName("search people"));
    this.searchButton = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("search").setExact(true));
    this.snackbar = page.getByRole(AriaRole.ALERT);
  }

  @Step("Open people page")
  public PeoplePage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Reload people page")
  public PeoplePage reload() {
    page.reload();
    return this;
  }

  public Header header() {
    return header;
  }

  public PeopleTable table() {
    return table;
  }

  @Step("Click All People tab")
  public PeoplePage clickAllPeopleTab() {
    allPeopleTab.click();
    return this;
  }

  @Step("Fill search with {query}")
  public PeoplePage fillSearch(String query) {
    searchInput.fill(query);
    return this;
  }

  @Step("Click Search")
  public PeoplePage clickSearch() {
    searchButton.click();
    return this;
  }

  @Step("Click Income invitations tab")
  public PeoplePage clickIncomeInvitationsTab() {
    incomeInvitationsTab.click();
    return this;
  }

  @Step("Click Outcome invitations tab")
  public PeoplePage clickOutcomeInvitationsTab() {
    outcomeInvitationsTab.click();
    return this;
  }

  @Step("Check that message '{message}' is shown")
  public PeoplePage checkMessage(String message) {
    assertThat(snackbar).hasText(message);
    return this;
  }
}
