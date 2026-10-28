package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.data.TestUser;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * For a test method annotated with {@link User}: opens a {@link UserFixture} before the test, injects its user as a
 * {@link TestUser} parameter and lets the store close the fixture once the test is over.
 */
public class UserExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    if (AnnotationSupport.isAnnotated(context.getRequiredTestMethod(), User.class)) {
      UserFixture fixture = new UserFixture();
      // Registered before create(): the store closes AutoCloseable values after the test, and that also covers a
      // failure between creating the user and the test start
      context.getStore(NAMESPACE).put(UserFixture.class, fixture);
      fixture.create();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == TestUser.class
        && AnnotationSupport.isAnnotated(extensionContext.getRequiredTestMethod(), User.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(UserFixture.class, UserFixture.class).user();
  }
}
