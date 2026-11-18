package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.MyTravelsPage;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Singleton
public final class PhotoCard {

  private final Provider<MyTravelsPage> myTravelsPage;
  private final Locator cards;

  @Inject
  public PhotoCard(Page page, Provider<MyTravelsPage> myTravelsPage) {
    this.myTravelsPage = myTravelsPage;
    this.cards = page.getByRole(AriaRole.ARTICLE);
  }

  @Step("Click like on the photo {description}")
  public MyTravelsPage clickLike(String description) {
    button(description, "like").click();
    return myTravelsPage.get();
  }

  @Step("Click Photo actions on the photo {description}")
  public MyTravelsPage clickPhotoActions(String description) {
    button(description, "Photo actions").click();
    return myTravelsPage.get();
  }

  @Step("Check that the photo {description} is shown")
  public PhotoCard checkShown(String description) {
    assertThat(root(description)).isVisible();
    return this;
  }

  @Step("Check that the photo {description} is taken in {country}")
  public PhotoCard checkCountry(String description, String country) {
    assertThat(root(description)).hasAccessibleName(country + " " + description);
    return this;
  }

  @Step("Check that the photo {description} has {total} likes")
  public PhotoCard checkLikes(String description, int total) {
    assertThat(root(description).getByText(Pattern.compile("^\\d+ likes?$")))
        .hasText(total + (total == 1 ? " like" : " likes"));
    return this;
  }

  private Locator button(String description, String name) {
    return root(description).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  private Locator root(String description) {
    return cards.filter(new Locator.FilterOptions().setHasText(description));
  }
}
