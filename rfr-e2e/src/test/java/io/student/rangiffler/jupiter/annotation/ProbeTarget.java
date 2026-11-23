package io.student.rangiffler.jupiter.annotation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Nested
@Tag("engine-kit")
public @interface ProbeTarget {

}
