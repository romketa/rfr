package io.student.rangiffler.jupiter.extension;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.impl.junit.PageExtension;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class PagesExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(PagesExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    Page page = PageExtension.getOrCreatePage(context);
    context.getStore(NAMESPACE).put(Injector.class,
        Guice.createInjector(binder -> binder.bind(Page.class).toInstance(page)));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().getPackageName()
        .equals(LandingPage.class.getPackageName());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    Injector injector = extensionContext.getStore(NAMESPACE).get(Injector.class, Injector.class);
    if (injector == null) {
      throw new ParameterResolutionException(
          "Injector for parameter " + parameterContext.getParameter().getName()
              + " of " + parameterContext.getDeclaringExecutable().getName()
              + " is missing from the store");
    }
    return injector.getInstance(parameterContext.getParameter().getType());
  }
}
