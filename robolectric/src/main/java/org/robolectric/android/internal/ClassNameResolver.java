package org.robolectric.android.internal;

public class ClassNameResolver<T> {
  private String packageName;
  private String className;

  public static Class<?> resolve(String packageName, String className)
      throws ClassNotFoundException {
    Class<?> aClass;
    if (looksFullyQualified(className)) {
      aClass = safeClassForName(className);
    } else {
      if (className.startsWith(".")) {
        aClass = safeClassForName(packageName + className);
      } else {
        aClass = safeClassForName(packageName + "." + className);
      }
    }

    if (aClass == null) {
      throw new ClassNotFoundException("Could not find a class for package: "
          + packageName + " and class name: " + className);
    }
    return aClass;
  }

  /**
   * @deprecated Use {@link #resolve(String, String)} instead.
   */
  @Deprecated
  public ClassNameResolver(String packageName, String className) {
    this.packageName = packageName;
    this.className = className;
  }

  /**
   * @deprecated Use {@link #resolve(String, String)} instead.
   */
  public Class<? extends T> resolve() throws ClassNotFoundException {
    return (Class<? extends T>) resolve(packageName, className);
  }

  private static boolean looksFullyQualified(String className) {
    return className.contains(".") && !className.startsWith(".");
  }

  private static Class<?> safeClassForName(String classNamePath) {
    try {
      return Class.forName(classNamePath);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
