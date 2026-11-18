package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import io.student.rangiffler.page.PeoplePage;

@Singleton
public final class Sidebar {

  private final Provider<PeoplePage> peoplePage;
  private final Locator peopleLink;

  @Inject
  public Sidebar(Page page, Provider<PeoplePage> peoplePage) {
    this.peoplePage = peoplePage;
    Locator root = page.getByTestId("sidebar");
    this.peopleLink = root.getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("People").setExact(true));
  }

  @Step("Click People")
  public PeoplePage clickPeople() {
    peopleLink.click();
    return peoplePage.get();
  }
}
