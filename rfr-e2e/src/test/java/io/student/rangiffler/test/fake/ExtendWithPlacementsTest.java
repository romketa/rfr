package io.student.rangiffler.test.fake;

import io.qameta.allure.Allure;
import io.student.rangiffler.jupiter.annotation.DemoTest;
import io.student.rangiffler.jupiter.extension.DemoExtension;
import io.student.rangiffler.jupiter.extension.DemoExtension.DemoValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExtendWithPlacementsTest {

  private static final String TEST_BODY_INDENT = "  ".repeat(3);

  private static void log(DemoValue... values) {
    String message = "test body runs with " + Arrays.toString(values);
    System.out.println(TEST_BODY_INDENT + message);
    Allure.step(message);
  }

  @ExtendWith(DemoExtension.class)
  abstract static class ExtendedSuperclass {

  }

  @ExtendWith(DemoExtension.class)
  interface ExtendedInterface {

  }

  @Nested
  @ExtendWith(DemoExtension.class)
  class OnClass {

    @Test
    void shouldResolveValueRegisteredOnClassInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnClassInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnSuperclass extends ExtendedSuperclass {

    @Test
    void shouldResolveValueInheritedFromSuperclassInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueInheritedFromSuperclassInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnInterface implements ExtendedInterface {

    @Test
    void shouldResolveValueRegisteredOnInterfaceInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnInterfaceInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  @DemoTest
  class OnComposedAnnotation {

    @Test
    void shouldResolveValueRegisteredByComposedAnnotationInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredByComposedAnnotationInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnMethod {

    @Test
    @ExtendWith(DemoExtension.class)
    void shouldResolveValueRegisteredOnMethodInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    @ExtendWith(DemoExtension.class)
    void shouldResolveValueRegisteredOnMethodInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnStaticField {

    @ExtendWith(DemoExtension.class)
    static DemoValue placement;

    @Test
    void shouldResolveValueRegisteredOnStaticFieldInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnStaticFieldInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnInstanceField {

    @ExtendWith(DemoExtension.class)
    DemoValue placement;

    @Test
    void shouldResolveValueRegisteredOnInstanceFieldInFirstTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnInstanceFieldInSecondTest(DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnConstructorParameter {

    private final DemoValue constructed;

    OnConstructorParameter(@ExtendWith(DemoExtension.class) DemoValue constructed) {
      this.constructed = constructed;
    }

    @Test
    void shouldResolveValueRegisteredOnConstructorParameterInFirstTest(DemoValue value) {
      log(constructed, value);
      assertNotNull(constructed);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnConstructorParameterInSecondTest(DemoValue value) {
      log(constructed, value);
      assertNotNull(constructed);
      assertNotNull(value);
    }
  }

  @Nested
  class OnTestMethodParameter {

    @Test
    void shouldResolveValueRegisteredOnTestMethodParameterInFirstTest(
        @ExtendWith(DemoExtension.class) DemoValue value) {
      log(value);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnTestMethodParameterInSecondTest(
        @ExtendWith(DemoExtension.class) DemoValue value) {
      log(value);
      assertNotNull(value);
    }
  }

  @Nested
  class OnLifecycleMethodParameter {

    private DemoValue prepared;

    @BeforeEach
    void prepare(@ExtendWith(DemoExtension.class) DemoValue prepared) {
      this.prepared = prepared;
    }

    @Test
    void shouldResolveValueRegisteredOnLifecycleMethodParameterInFirstTest(DemoValue value) {
      log(prepared, value);
      assertNotNull(prepared);
      assertNotNull(value);
    }

    @Test
    void shouldResolveValueRegisteredOnLifecycleMethodParameterInSecondTest(DemoValue value) {
      log(prepared, value);
      assertNotNull(prepared);
      assertNotNull(value);
    }
  }
}
