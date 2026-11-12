package io.student.rangiffler.jupiter.extension;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
    AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback,
    TestInstancePreConstructCallback, TestInstancePreDestroyCallback, ParameterResolver {

  public record DemoValue(int extension, String resolvedFor) {

  }

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
      DemoExtension.class);
  private static final AtomicInteger INSTANCES = new AtomicInteger();

  private final int id = INSTANCES.incrementAndGet();
  private boolean announced;

  @Override
  public void beforeAll(ExtensionContext context) {
    trace("beforeAll", context);
  }

  @Override
  public void preConstructTestInstance(TestInstanceFactoryContext factoryContext,
      ExtensionContext context) {
    trace("preConstructTestInstance", context);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    trace("beforeEach", context);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    trace("beforeTestExecution", context);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    trace("afterTestExecution", context);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    trace("afterEach", context);
  }

  @Override
  public void preDestroyTestInstance(ExtensionContext context) {
    trace("preDestroyTestInstance", context);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    trace("afterAll", context);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == DemoValue.class;
  }

  @Override
  public DemoValue resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    Executable executable = parameterContext.getDeclaringExecutable();
    String target = (executable instanceof Constructor<?>
        ? executable.getDeclaringClass().getSimpleName() : executable.getName())
        + "[" + parameterContext.getIndex() + "]";
    trace("resolveParameter for " + target, extensionContext);
    return new DemoValue(id, target);
  }

  private void trace(String callback, ExtensionContext context) {
    header(context);
    int depth = depth(context) + 1;
    if (!announced) {
      announced = true;
      line(depth, "new DemoExtension#" + id);
    }
    String message = "DemoExtension#" + id + " " + callback;
    line(depth, message);
    Allure.getLifecycle().getCurrentExecutableKey().ifPresent(key -> Allure.step(message));
  }

  private static void header(ExtensionContext context) {
    if (context.getParent().isEmpty()) {
      return;
    }
    header(context.getParent().get());
    context.getStore(NAMESPACE).computeIfAbsent(context.getUniqueId(), key -> {
      line(depth(context), context.getDisplayName());
      return Boolean.TRUE;
    });
  }

  private static int depth(ExtensionContext context) {
    return context.getParent().map(parent -> depth(parent) + 1).orElse(-1);
  }

  private static void line(int depth, String text) {
    System.out.println("  ".repeat(depth) + text);
  }
}
