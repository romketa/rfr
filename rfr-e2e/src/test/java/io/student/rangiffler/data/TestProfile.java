package io.student.rangiffler.data;

public record TestProfile(String firstname, String surname, String country) {

  private static final String DEFAULT_COUNTRY = "ru";

  public static TestProfile defaults() {
    return new TestProfile("", "", DEFAULT_COUNTRY);
  }

  public boolean isRequested() {
    return !firstname.isEmpty() || !surname.isEmpty() || !country.isEmpty();
  }

  public String countryOrDefault() {
    return country.isEmpty() ? DEFAULT_COUNTRY : country;
  }
}
