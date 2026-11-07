package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.PeoplePage;

public final class Sidebar {

  private final Page page;
  private final Locator peopleLink;

  public Sidebar(Page page) {
    this.page = page;
    Locator root = page.locator(".MuiDrawer-paper");
    this.peopleLink = root.getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("People").setExact(true));
  }

  @Step("Click People")
  public PeoplePage clickPeople() {
    peopleLink.click();
    return new PeoplePage(page);
  }
}
