package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.PeoplePage;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Singleton
public final class PeopleRow {

  private final Page page;
  private final Provider<PeoplePage> peoplePage;
  private final Locator rows;

  @Inject
  public PeopleRow(Page page, Provider<PeoplePage> peoplePage) {
    this.page = page;
    this.peoplePage = peoplePage;
    this.rows = page.getByRole(AriaRole.TABPANEL).getByTestId("people-row");
  }

  @Step("Click Add for {username}")
  public PeoplePage clickAdd(String username) {
    return click(username, "Add");
  }

  @Step("Click Accept for {username}")
  public PeoplePage clickAccept(String username) {
    return click(username, "Accept");
  }

  @Step("Click Cancel for {username}")
  public PeoplePage clickCancel(String username) {
    return click(username, "Cancel");
  }

  @Step("Check that {username} is shown")
  public PeopleRow checkShown(String username) {
    assertThat(root(username)).isVisible();
    return this;
  }

  @Step("Check that the actions of {username} are {buttons}")
  public PeopleRow checkActions(String username, String... buttons) {
    assertThat(actions(username)).hasText(buttons);
    return this;
  }

  @Step("Check that {username} is marked as waiting")
  public PeopleRow checkWaiting(String username) {
    assertThat(root(username).getByText("Waiting...")).isVisible();
    return this;
  }

  Locator rows() {
    return rows;
  }

  private PeoplePage click(String username, String button) {
    actions(username).filter(new Locator.FilterOptions().setHasText(button)).click();
    return peoplePage.get();
  }

  private Locator actions(String username) {
    return root(username).getByRole(AriaRole.BUTTON);
  }

  private Locator root(String username) {
    return rows.filter(new Locator.FilterOptions().setHas(
        page.getByText(username, new Page.GetByTextOptions().setExact(true))));
  }
}
