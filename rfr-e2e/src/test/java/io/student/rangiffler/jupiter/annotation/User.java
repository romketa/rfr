package io.student.rangiffler.jupiter.annotation;

import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code TestUser} parameter: a new user is created in the auth database for every annotated parameter
 * before the test and removed from both schemas afterwards, so one test can get several independent users.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(UserExtension.class)
public @interface User {
}
