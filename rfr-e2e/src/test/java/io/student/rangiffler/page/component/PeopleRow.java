package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.PeoplePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleRow {

  private final Page page;
  private final String username;
  private final Locator root;
  private final Locator actions;
  private final Locator waitingChip;

  PeopleRow(Page page, Locator rows, String username) {
    this.page = page;
    this.username = username;
    this.root = rows.filter(new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.CELL,
            new Page.GetByRoleOptions().setName(username).setExact(true))));
    this.actions = root.getByRole(AriaRole.BUTTON);
    this.waitingChip = root.getByText("Waiting...");
  }

  @Step("Click Add for {this.username}")
  public PeoplePage clickAdd() {
    return click("Add");
  }

  @Step("Click Accept for {this.username}")
  public PeoplePage clickAccept() {
    return click("Accept");
  }

  @Step("Click Cancel for {this.username}")
  public PeoplePage clickCancel() {
    return click("Cancel");
  }

  @Step("Check that {this.username} is shown")
  public PeopleRow checkShown() {
    assertThat(root).isVisible();
    return this;
  }

  @Step("Check that the actions of {this.username} are {buttons}")
  public PeopleRow checkActions(String... buttons) {
    assertThat(actions).hasText(buttons);
    return this;
  }

  @Step("Check that {this.username} is marked as waiting")
  public PeopleRow checkWaiting() {
    assertThat(waitingChip).isVisible();
    return this;
  }

  private PeoplePage click(String button) {
    actions.filter(new Locator.FilterOptions().setHasText(button)).click();
    return new PeoplePage(page);
  }
}
