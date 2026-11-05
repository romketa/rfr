package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Resolves every {@link TestUser} parameter annotated with {@link User} to a user of its own {@link UserFixture};
 * the store closes each fixture once the test is over.
 */
public class UserExtension implements ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(User.class) && parameterContext.getParameter().getType() == TestUser.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    UserFixture fixture = new UserFixture();
    // Keyed by the parameter itself, unique across the test and its lifecycle methods, so no fixture replaces
    // another. Registered before create(): the store closes AutoCloseable values after the test, and that also covers
    // a failure between creating the user and the test start
    extensionContext.getStore(NAMESPACE).put(parameterContext.getParameter(), fixture);
    fixture.create();
    return fixture.user();
  }
}
