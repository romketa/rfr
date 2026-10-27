package io.student.rangiffler.jupiter.annotation;

import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Creates a user in the auth database before the test and removes it afterwards.
 * The test receives it as a {@code TestUser} method parameter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(UserExtension.class)
public @interface User {
}
