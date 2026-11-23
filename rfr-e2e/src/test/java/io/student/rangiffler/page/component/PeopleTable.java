package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Singleton
public final class PeopleTable {

  private final PeopleRow row;
  private final Locator rows;
  private final Locator emptyMessage;

  @Inject
  public PeopleTable(Page page, PeopleRow row) {
    this.row = row;
    this.rows = row.rows();
    this.emptyMessage = page.getByRole(AriaRole.TABPANEL).getByText("There are no users yet");
  }

  public PeopleRow row() {
    return row;
  }

  @Step("Check that the table shows exactly {usernames}")
  public PeopleTable checkPeople(String... usernames) {
    assertThat(rows).hasCount(usernames.length);
    for (String username : usernames) {
      row.checkShown(username);
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
