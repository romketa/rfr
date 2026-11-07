package io.student.rangiffler.jupiter.annotation;

import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code TestUser} parameter: a new user is created in the auth database for every
 * annotated parameter before the test and removed from both schemas afterwards, so one test can get
 * several independent users. Any profile attribute, friend or income invitation also creates the
 * rfr-api profile up front; an omitted country falls back to {@code ru}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(UserExtension.class)
public @interface User {

  String firstname() default "";

  String surname() default "";

  String country() default "";

  int friends() default 0;

  int incomeInvitations() default 0;
}
