package org.robolectric;

import android.app.Application;
import java.lang.reflect.Method;
import org.robolectric.android.ApplicationTestUtil;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;

public interface TestLifecycle<T extends Application> {

  /**
   * This method creates an application of the class indicated by
   * {@link #determineApplicationClass(Method, AndroidManifest, Config)}.
   *
   * @param method The currently-running test method.
   * @param appManifest The application manifest.
   * @param config The configuration annotation from the test if present.
   * @return An instance of the Application class specified by the ApplicationManifest.xml or an
   * instance of Application if not specified.
   * @deprecated Implement {@link #determineApplicationClass(Method, AndroidManifest, Config)} instead.
   */
  @Deprecated
  default T createApplication(Method method, AndroidManifest appManifest, Config config) {
    Class<T> applicationClass = determineApplicationClass(method, appManifest, config);
    return ApplicationTestUtil.newApplication(applicationClass);
  }

  /**
   * Override this method if you want to provide your own implementation of Application.
   *
   * This method attempts to instantiate an application instance as follows:
   *
   * 1. If specified loads the application specified in the Config annotation
   * 1. Attempt to load a test application as documented <a href="http://robolectric.blogspot.com/2013/04/the-test-lifecycle-in-20.html">here</a>
   * 1. Use the application as specified in the AndroidManifest.xml
   * 1. Instantiate a standard {@link android.app.Application}
   *
   * @param method The currently-running test method.
   * @param appManifest The application manifest.
   * @param config The configuration annotation from the test if present.
   * @return An instance of the Application class specified by the ApplicationManifest.xml or an instance of
   *         Application if not specified.
   */
  Class<T> determineApplicationClass(Method method, AndroidManifest appManifest, Config config);

  void beforeTest(Method method);

  void prepareTest(Object test);

  void afterTest(Method method);
}
