package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.MyTravelsPage;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PhotoCard {

  private final Page page;
  private final String description;
  private final Locator root;
  private final Locator likeButton;
  private final Locator photoActionsButton;
  private final Locator likes;

  public PhotoCard(Page page, Locator cards, String description) {
    this.page = page;
    this.description = description;
    this.root = cards.filter(new Locator.FilterOptions().setHasText(description));
    this.likeButton = root.getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("like").setExact(true));
    this.photoActionsButton = root.getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Photo actions").setExact(true));
    this.likes = root.getByText(Pattern.compile("^\\d+ likes?$"));
  }

  @Step("Click like on the photo {this.description}")
  public MyTravelsPage clickLike() {
    likeButton.click();
    return new MyTravelsPage(page);
  }

  @Step("Click Photo actions on the photo {this.description}")
  public MyTravelsPage clickPhotoActions() {
    photoActionsButton.click();
    return new MyTravelsPage(page);
  }

  @Step("Check that the photo {this.description} is shown")
  public PhotoCard checkShown() {
    assertThat(root).isVisible();
    return this;
  }

  @Step("Check that the photo {this.description} is taken in {country}")
  public PhotoCard checkCountry(String country) {
    assertThat(root).hasAccessibleName(country + " " + description);
    return this;
  }

  @Step("Check that the photo {this.description} has {total} likes")
  public PhotoCard checkLikes(int total) {
    assertThat(likes).hasText(total + (total == 1 ? " like" : " likes"));
    return this;
  }
}
