package io.student.rangiffler.test.travels;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WebTest
@DisplayName("My travels на mock-ленте")
public class MyTravelsMockTest {

  private static final int OWN_PHOTOS = 4;
  private static final int OWN_COUNTRIES = 4;
  private static final int PHOTOS_WITH_FRIENDS = 7;
  private static final int COUNTRIES_WITH_FRIENDS = 7;
  private static final String OWN_PHOTO = "Амстердам!";
  private static final String FRIEND_PHOTO = "Старая Прага завораживает!";
  private static final String FRIEND_PHOTO_COUNTRY = "Czech Republic";
  private static final int FRIEND_PHOTO_COUNTRY_PHOTOS = 1;

  @Test
  @DisplayName("Переключатель меняет число карточек и счетчик стран на mock-ленте")
  void shouldChangePhotosAndCountriesWhenToggled(@User TestUser user, LandingPage landingPage) {
    login(user, landingPage)
        .checkPhotosCount(OWN_PHOTOS)
        .checkVisitedCountries(OWN_COUNTRIES)
        .clickWithFriends()
        .checkPhotosCount(PHOTOS_WITH_FRIENDS)
        .checkVisitedCountries(COUNTRIES_WITH_FRIENDS)
        .clickOnlyMyTravels()
        .checkPhotosCount(OWN_PHOTOS)
        .checkVisitedCountries(OWN_COUNTRIES);
  }

  @Test
  @DisplayName("Фильтр по стране оставляет одну карточку и сбрасывается на mock-ленте")
  void shouldFilterPhotosByCountryAndReset(@User TestUser user, LandingPage landingPage) {
    login(user, landingPage)
        .clickWithFriends()
        .checkPhotosCount(PHOTOS_WITH_FRIENDS)
        .clickVisitedCountries()
        .selectCountry(FRIEND_PHOTO_COUNTRY)
        .checkCountryFilter(FRIEND_PHOTO_COUNTRY)
        .checkPhotosCount(FRIEND_PHOTO_COUNTRY_PHOTOS)
        .clickResetCountryFilter()
        .checkNoCountryFilter()
        .checkPhotosCount(PHOTOS_WITH_FRIENDS);
  }

  @Test
  @DisplayName("Меню Photo actions есть только у своих фото на mock-ленте")
  void shouldOfferPhotoActionsOnlyForOwnPhotos(@User TestUser user, LandingPage landingPage) {
    login(user, landingPage)
        .clickWithFriends()
        .checkPhotosCount(PHOTOS_WITH_FRIENDS)
        .photo()
        .checkPhotoActions(OWN_PHOTO)
        .checkCountry(FRIEND_PHOTO, FRIEND_PHOTO_COUNTRY)
        .checkNoPhotoActions(FRIEND_PHOTO);
  }

  private static MyTravelsPage login(TestUser user, LandingPage landingPage) {
    return landingPage
        .open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password())
        .clickSignIn();
  }
}
