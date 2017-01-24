package org.robolectric.internal;

import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.robolectric.internal.bytecode.ClassHandler;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;
import org.robolectric.internal.bytecode.InstrumentingClassLoader;
import org.robolectric.internal.bytecode.Interceptor;
import org.robolectric.internal.bytecode.Interceptors;
import org.robolectric.internal.bytecode.InvokeDynamicSupport;
import org.robolectric.internal.bytecode.RoboConfig;
import org.robolectric.internal.bytecode.RobolectricInternals;
import org.robolectric.internal.bytecode.Sandbox;
import org.robolectric.internal.bytecode.ShadowInvalidator;
import org.robolectric.internal.bytecode.ShadowMap;
import org.robolectric.internal.bytecode.ShadowWrangler;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.robolectric.util.ReflectionHelpers.newInstance;
import static org.robolectric.util.ReflectionHelpers.setStaticField;

public class InstrumentingTestRunner extends BlockJUnit4ClassRunner {

  private final Interceptors interceptors;
  private final HashSet<Class<?>> loadedTestClasses = new HashSet<>();

  public InstrumentingTestRunner(Class<?> klass) throws InitializationError {
    super(klass);
    interceptors = new Interceptors(findInterceptors());
  }

  @NotNull
  protected Collection<Interceptor> findInterceptors() {
    return Collections.emptyList();
  }

  @NotNull
  public Interceptors getInterceptors() {
    return interceptors;
  }

  @Override
  protected Statement classBlock(RunNotifier notifier) {
    final Statement statement = childrenInvoker(notifier);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
          for (Class<?> testClass : loadedTestClasses) {
            invokeAfterClass(testClass);
          }
        } finally {
          afterClass();
          loadedTestClasses.clear();
        }
      }
    };
  }

  private void invokeBeforeClass(final Class clazz) throws Throwable {
    if (!loadedTestClasses.contains(clazz)) {
      loadedTestClasses.add(clazz);

      final TestClass testClass = new TestClass(clazz);
      final List<FrameworkMethod> befores = testClass.getAnnotatedMethods(BeforeClass.class);
      for (FrameworkMethod before : befores) {
        before.invokeExplosively(null);
      }
    }
  }

  private static void invokeAfterClass(final Class<?> clazz) throws Throwable {
    final TestClass testClass = new TestClass(clazz);
    final List<FrameworkMethod> afters = testClass.getAnnotatedMethods(AfterClass.class);
    for (FrameworkMethod after : afters) {
      after.invokeExplosively(null);
    }
  }

  protected void afterClass() {
  }

  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier) {
    Description description = describeChild(method);
    EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);

    if (shouldIgnore(method)) {
      eachNotifier.fireTestIgnored();
    } else {
      eachNotifier.fireTestStarted();

      try {
        methodBlock(method).evaluate();
      } catch (AssumptionViolatedException e) {
        eachNotifier.addFailedAssumption(e);
      } catch (Throwable e) {
        eachNotifier.addFailure(e);
      } finally {
        eachNotifier.fireTestFinished();
      }
    }
  }

  @NotNull
  protected Sandbox getSandbox(FrameworkMethod method) {
    InstrumentationConfiguration instrumentationConfiguration = createClassLoaderConfig(method);
    ClassLoader instrumentingClassLoader = new InstrumentingClassLoader(instrumentationConfiguration);
    Sandbox sandbox = new Sandbox(instrumentingClassLoader);
    configureShadows(method, sandbox);
    return sandbox;
  }

  /**
   * Create an {@link InstrumentationConfiguration} suitable for the provided {@link FrameworkMethod}.
   *
   * Custom TestRunner subclasses may wish to override this method to provide alternate configuration.
   *
   * @param method the test method that's about to run
   * @return an {@link InstrumentationConfiguration}
   */
  @NotNull
  protected InstrumentationConfiguration createClassLoaderConfig(FrameworkMethod method) {
    InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder()
        .doNotAcquirePackage("java.")
        .doNotAcquirePackage("sun.")
        .doNotAcquirePackage("org.robolectric.annotation.")
        .doNotAcquirePackage("org.robolectric.internal.")
        .doNotAcquirePackage("org.robolectric.util.")
        .doNotAcquirePackage("org.junit.");

    for (Class<?> shadowClass : getExtraShadows(method)) {
      ShadowMap.ShadowInfo shadowInfo = ShadowMap.getShadowInfo(shadowClass);
      builder.addInstrumentedClass(shadowInfo.getShadowedClassName());
    }

    return builder.build();
  }

  protected void configureShadows(FrameworkMethod method, Sandbox sandbox) {
    ShadowMap.Builder builder = createShadowMap().newBuilder();

    // Configure shadows *BEFORE* setting the ClassLoader. This is necessary because
    // creating the ShadowMap loads all ShadowProviders via ServiceLoader and this is
    // not available once we install the Robolectric class loader.
    Class<?>[] shadows = getExtraShadows(method);
    if (shadows.length > 0) {
      builder.addShadowClasses(shadows);
    }
    ShadowMap shadowMap = builder.build();
    sandbox.replaceShadowMap(shadowMap);

    sandbox.classHandler = createClassHandler(shadowMap, sandbox);
  }

  protected Statement methodBlock(final FrameworkMethod method) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Sandbox sandbox = getSandbox(method);
        injectEnvironment(sandbox);

        final ClassLoader priorContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(sandbox.getRobolectricClassLoader());

        Class bootstrappedTestClass = sandbox.bootstrappedClass(getTestClass().getJavaClass());
        HelperTestRunner helperTestRunner = getHelperTestRunner(bootstrappedTestClass);

        final Method bootstrappedMethod;
        try {
          //noinspection unchecked
          bootstrappedMethod = bootstrappedTestClass.getMethod(method.getMethod().getName());
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        }

        try {
          // Only invoke @BeforeClass once per class
          invokeBeforeClass(bootstrappedTestClass);

          beforeTest(sandbox, method, bootstrappedMethod);

          final Statement statement = helperTestRunner.methodBlock(new FrameworkMethod(bootstrappedMethod));

          // todo: this try/finally probably isn't right -- should mimic RunAfters? [xw]
          try {
            statement.evaluate();
          } finally {
            afterTest(method, bootstrappedMethod);
          }
        } finally {
          Thread.currentThread().setContextClassLoader(priorContextClassLoader);
          finallyAfterTest();
        }
      }
    };
  }

  protected void beforeTest(Sandbox sandbox, FrameworkMethod method, Method bootstrappedMethod) throws Throwable {
  }

  protected void afterTest(FrameworkMethod method, Method bootstrappedMethod) {
  }

  protected void finallyAfterTest() {
  }

  protected HelperTestRunner getHelperTestRunner(Class bootstrappedTestClass) {
    try {
      return new HelperTestRunner(bootstrappedTestClass);
    } catch (InitializationError initializationError) {
      throw new RuntimeException(initializationError);
    }
  }

  protected static class HelperTestRunner extends BlockJUnit4ClassRunner {
    public HelperTestRunner(Class<?> klass) throws InitializationError {
      super(klass);
    }

    // cuz accessibility
    @Override
    protected Statement methodBlock(FrameworkMethod method) {
      return super.methodBlock(method);
    }
  }

  @NotNull
  protected Class<?>[] getExtraShadows(FrameworkMethod method) {
    List<Class<?>> shadowClasses = new ArrayList<>();
    addShadows(shadowClasses, getTestClass().getAnnotation(RoboConfig.class));
    addShadows(shadowClasses, method.getAnnotation(RoboConfig.class));
    return shadowClasses.toArray(new Class[shadowClasses.size()]);
  }

  private void addShadows(List<Class<?>> shadowClasses, RoboConfig annotation) {
    if (annotation != null) {
      shadowClasses.addAll(asList(annotation.shadows()));
    }
  }

  protected ShadowMap createShadowMap() {
    return ShadowMap.EMPTY;
  }

  @NotNull
  protected ClassHandler createClassHandler(ShadowMap shadowMap, Sandbox sandbox) {
    return new ShadowWrangler(shadowMap, 0, interceptors);
  }

  public void injectEnvironment(Sandbox sandbox) {
    ClassLoader robolectricClassLoader = sandbox.getRobolectricClassLoader();
    ClassHandler classHandler = sandbox.getClassHandler();
    ShadowInvalidator invalidator = sandbox.getShadowInvalidator();

    Class<?> robolectricInternalsClass = ReflectionHelpers.loadClass(robolectricClassLoader, RobolectricInternals.class.getName());
    ReflectionHelpers.setStaticField(robolectricInternalsClass, "classHandler", classHandler);
    ReflectionHelpers.setStaticField(robolectricInternalsClass, "shadowInvalidator", invalidator);
    ReflectionHelpers.setStaticField(robolectricInternalsClass, "classLoader", robolectricClassLoader);

    Class<?> invokeDynamicSupportClass = ReflectionHelpers.loadClass(robolectricClassLoader, InvokeDynamicSupport.class.getName());
    setStaticField(invokeDynamicSupportClass, "INTERCEPTORS", interceptors);

    Class<?> shadowClass = ReflectionHelpers.loadClass(robolectricClassLoader, Shadow.class.getName());
    setStaticField(shadowClass, "SHADOW_IMPL",
        newInstance(ReflectionHelpers.loadClass(robolectricClassLoader, ShadowImpl.class.getName())));
  }

  protected boolean shouldIgnore(FrameworkMethod method) {
    return method.getAnnotation(Ignore.class) != null;
  }

}
