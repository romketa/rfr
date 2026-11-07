package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestProfile;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.UserRequest;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Creates a {@link UserFixture} for every {@link TestUser} parameter of the test method annotated
 * with {@link User} before the test and resolves the parameter to its user; the store closes each
 * fixture once the test is over.
 */
public class UserExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
      UserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    Arrays.stream(context.getRequiredTestMethod().getParameters())
        .filter(parameter -> isUserParameter(parameter, context))
        .forEach(parameter -> {
          UserFixture fixture = request(parameter.getAnnotation(User.class)).fixture();
          store.put(parameter, fixture);
          fixture.create();
        });
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    return isUserParameter(parameterContext.getParameter(), extensionContext);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    UserFixture fixture = extensionContext.getStore(NAMESPACE)
        .get(parameterContext.getParameter(), UserFixture.class);
    if (fixture == null) {
      throw new ParameterResolutionException(
          "UserFixture for parameter " + parameterContext.getParameter().getName()
              + " of " + parameterContext.getDeclaringExecutable().getName()
              + " is missing from the store");
    }
    return fixture.user();
  }

  private static UserRequest request(User user) {
    return new UserRequest(new TestProfile(user.firstname(), user.surname(), user.country()),
        user.friends(), user.incomeInvitations());
  }

  private static boolean isUserParameter(Parameter parameter, ExtensionContext context) {
    return parameter.isAnnotationPresent(User.class)
        && parameter.getType() == TestUser.class
        && context.getTestMethod().filter(parameter.getDeclaringExecutable()::equals).isPresent();
  }
}
