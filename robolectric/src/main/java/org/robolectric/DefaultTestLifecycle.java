package org.robolectric;

import android.app.Application;
import java.lang.reflect.Method;
import org.robolectric.android.ApplicationTestUtil;
import org.robolectric.android.internal.ClassNameResolver;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;

public class DefaultTestLifecycle implements TestLifecycle {

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Application> determineApplicationClass(Method method,
      AndroidManifest appManifest, Config config) {
    if (config != null && !Config.Builder.isDefaultApplication(config.application())
        && config.application().getCanonicalName() != null) {
      try {
        return resolve(null, config.application().getName());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else if (appManifest != null && appManifest.getApplicationName() != null) {
      String packageName = appManifest.getPackageName();
      String applicationName = appManifest.getApplicationName();

      try {
        return resolve(packageName, getTestApplicationName(applicationName));
      } catch (ClassNotFoundException e) {
        // no problem
      }

      try {
        return resolve(packageName, applicationName);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      return Application.class;
    }
  }

  private static Class<? extends Application> resolve(String packageName, String applicationName)
      throws ClassNotFoundException {
    return (Class<? extends Application>) ClassNameResolver.resolve(packageName, applicationName);
  }

  /**
   * Called before each test method is run.
   *
   * @param method the test method about to be run
   */
  @Override public void beforeTest(final Method method) {
    if (RuntimeEnvironment.application instanceof TestLifecycleApplication) {
      ((TestLifecycleApplication) RuntimeEnvironment.application).beforeTest(method);
    }
  }

  @Override public void prepareTest(final Object test) {
    if (RuntimeEnvironment.application instanceof TestLifecycleApplication) {
      ((TestLifecycleApplication) RuntimeEnvironment.application).prepareTest(test);
    }
  }

  /**
   * Called after each test method is run.
   *
   * @param method the test method that just ran.
   */
  @Override public void afterTest(final Method method) {
    if (RuntimeEnvironment.application instanceof TestLifecycleApplication) {
      ((TestLifecycleApplication) RuntimeEnvironment.application).afterTest(method);
    }
  }

  /**
   * @deprecated Do not use.
   */
  @Deprecated
  public String getTestApplicationName(String applicationName) {
    int lastDot = applicationName.lastIndexOf('.');
    if (lastDot > -1) {
      return applicationName.substring(0, lastDot) + ".Test" + applicationName.substring(lastDot + 1);
    } else {
      return "Test" + applicationName;
    }
  }
}
