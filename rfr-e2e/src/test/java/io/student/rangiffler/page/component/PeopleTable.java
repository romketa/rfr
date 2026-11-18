package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleTable {

  private final Page page;
  private final Locator rows;
  private final Locator emptyMessage;

  public PeopleTable(Page page) {
    this.page = page;
    Locator root = page.getByRole(AriaRole.TABPANEL);
    this.rows = root.getByTestId("people-row");
    this.emptyMessage = root.getByText("There are no users yet");
  }

  public PeopleRow row(String username) {
    return new PeopleRow(page, rows, username);
  }

  @Step("Check that the table shows exactly {usernames}")
  public PeopleTable checkPeople(String... usernames) {
    assertThat(rows).hasCount(usernames.length);
    for (String username : usernames) {
      row(username).checkShown();
    }
    return this;
  }

  @Step("Check that the table is empty")
  public PeopleTable checkNoPeople() {
    assertThat(rows).hasCount(0);
    assertThat(emptyMessage).isVisible();
    return this;
  }
}
