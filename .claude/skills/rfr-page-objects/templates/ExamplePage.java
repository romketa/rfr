package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.page.component.ExampleComponent;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Template of a page object. Replace "Example" with the page name, keep the structure:
 * URL constant, private final locators, composed components, open(), actions, checks.
 */
@Singleton
public final class ExamplePage {

  public static final String URL = Config.frontUrl() + "/example";

  private final Page page;
  private final ExampleComponent header;
  private final Locator nameInput;
  private final Locator submitButton;

  @Inject
  public ExamplePage(Page page, ExampleComponent header) {
    this.page = page;
    this.header = header;
    this.nameInput = page.getByLabel("Name");
    this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"));
  }

  @Step("Open example page")
  public ExamplePage open() {
    page.navigate(URL);
    return this;
  }

  public ExampleComponent header() {
    return header;
  }

  @Step("Fill example form with name {name}")
  public ExamplePage fillExampleForm(String name) {
    nameInput.fill(name);
    return this;
  }

  @Step("Click Save")
  public ExamplePage clickSave() {
    submitButton.click();
    return this;
  }

  @Step("Check that name {name} is shown")
  public ExamplePage checkName(String name) {
    assertThat(nameInput).hasValue(name);
    return this;
  }
}
