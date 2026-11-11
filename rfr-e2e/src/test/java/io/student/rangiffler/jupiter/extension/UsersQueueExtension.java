package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.StaticUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.Arrays;

public class UsersQueueExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
      UsersQueueExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    Arrays.stream(context.getRequiredTestMethod().getParameters())
        .filter(parameter -> isStaticUserParameter(parameter, context))
        .forEach(parameter -> {
          UserFixture fixture = new StaticUserFixture();
          store.put(parameter, fixture);
          fixture.create();
        });
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    return isStaticUserParameter(parameterContext.getParameter(), extensionContext);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    UserFixture fixture = extensionContext.getStore(NAMESPACE)
        .get(parameterContext.getParameter(), UserFixture.class);
    if (fixture == null) {
      throw new ParameterResolutionException(
          "StaticUserFixture for parameter " + parameterContext.getParameter().getName()
              + " of " + parameterContext.getDeclaringExecutable().getName()
              + " is missing from the store");
    }
    return fixture.user();
  }

  private static boolean isStaticUserParameter(Parameter parameter, ExtensionContext context) {
    return parameter.isAnnotationPresent(StaticUser.class)
        && parameter.getType() == TestUser.class
        && context.getTestMethod().filter(parameter.getDeclaringExecutable()::equals).isPresent();
  }
}
