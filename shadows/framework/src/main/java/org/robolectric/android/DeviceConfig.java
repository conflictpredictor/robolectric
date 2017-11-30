package org.robolectric.android;

import static com.google.common.base.Strings.isNullOrEmpty;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import java.util.Locale;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.res.Qualifiers;
import org.robolectric.res.android.ConfigDescription;
import org.robolectric.res.android.ResTable_config;

class DeviceConfig {

  static void applyToConfiguration(Qualifiers qualifiers, int apiLevel,
      Configuration configuration, DisplayMetrics displayMetrics) {
    ResTable_config resTab = qualifiers.getConfig();

    if (resTab.mcc != 0) {
      configuration.mcc = resTab.mcc;
    }

    if (resTab.mnc != 0) {
      configuration.mnc = resTab.mnc;
    }

    // screenLayout includes size, long, layoutdir, and round.
    // layoutdir may be overridden by setLocale(), so do this first:
    int screenLayoutSize = getScreenLayoutSize(configuration);
    int resTabSize = resTab.screenLayoutSize();
    if (resTabSize != ResTable_config.SCREENSIZE_ANY) {
      screenLayoutSize = resTabSize;
    }

    int screenLayoutLong = getScreenLayoutLong(configuration);
    int resTabLong = resTab.screenLayoutLong();
    if (resTabLong != ResTable_config.SCREENLONG_ANY) {
      screenLayoutLong = resTabLong;
    }

    int screenLayoutLayoutDir = getScreenLayoutLayoutDir(configuration);
    int resTabLayoutDir = resTab.screenLayoutDirection();
    if (resTabLayoutDir != ResTable_config.LAYOUTDIR_ANY) {
      screenLayoutLayoutDir = resTabLayoutDir;
    }

    int screenLayoutRound = getScreenLayoutRound(configuration);
    int resTabRound = resTab.screenLayoutRound();
    if (resTabRound != ResTable_config.SCREENROUND_ANY) {
      screenLayoutRound = resTabRound << 8;
    }

    configuration.screenLayout =
        screenLayoutSize | screenLayoutLong | screenLayoutLayoutDir | screenLayoutRound;

    // locale...
    String lang = resTab.languageString();
    String region = resTab.regionString();
    String script = resTab.scriptString();

    Locale locale;
    if (isNullOrEmpty(lang) && isNullOrEmpty(region) && isNullOrEmpty(script)) {
      locale = null;
    } else {
      locale = new Locale.Builder()
          .setLanguage(lang)
          .setRegion(region)
          .setScript(script == null ? "" : script)
          .build();
    }
    if (locale != null) {
      setLocale(apiLevel, configuration, locale);
    }

    if (resTab.smallestScreenWidthDp != 0) {
      configuration.smallestScreenWidthDp = resTab.smallestScreenWidthDp;
    }

    if (resTab.screenWidthDp != 0) {
      configuration.screenWidthDp = resTab.screenWidthDp;
    }

    if (resTab.screenHeightDp != 0) {
      configuration.screenHeightDp = resTab.screenHeightDp;
    }

    if (resTab.orientation != ResTable_config.ORIENTATION_ANY) {
      configuration.orientation = resTab.orientation;
    }

    // uiMode includes type and night...
    int uiModeType = getUiModeType(configuration);
    int resTabType = resTab.uiModeType();
    if (resTabType != ResTable_config.UI_MODE_TYPE_ANY) {
      uiModeType = resTabType;
    }

    int uiModeNight = getUiModeNight(configuration);
    int resTabNight = resTab.uiModeNight();
    if (resTabNight != ResTable_config.UI_MODE_NIGHT_ANY) {
      uiModeNight = resTabNight;
    }
    configuration.uiMode = uiModeType | uiModeNight;

    if (resTab.density != ResTable_config.DENSITY_DEFAULT) {
      setDensity(resTab.density, apiLevel, configuration, displayMetrics);
    }

    if (resTab.touchscreen != ResTable_config.TOUCHSCREEN_ANY) {
      configuration.touchscreen = resTab.touchscreen;
    }

    if (resTab.keyboard != ResTable_config.KEYBOARD_ANY) {
      configuration.keyboard = resTab.keyboard;
    }

    if (resTab.keyboardHidden() != ResTable_config.KEYSHIDDEN_ANY) {
      configuration.keyboardHidden = resTab.keyboardHidden();
    }

    if (resTab.navigation != ResTable_config.NAVIGATION_ANY) {
      configuration.navigation = resTab.navigation;
    }

    if (resTab.navigationHidden() != ResTable_config.NAVHIDDEN_ANY) {
      configuration.navigationHidden = resTab.navigationHidden();
    }
  }

  private static void setDensity(int densityDpi, int apiLevel, Configuration configuration,
      DisplayMetrics displayMetrics) {
    if (apiLevel >= VERSION_CODES.JELLY_BEAN_MR1) {
      configuration.densityDpi = densityDpi;
    }
    displayMetrics.densityDpi = densityDpi;
    displayMetrics.density = displayMetrics.densityDpi * DisplayMetrics.DENSITY_DEFAULT_SCALE;
  }

  enum ScreenSize {
    small(320, 426, Configuration.SCREENLAYOUT_SIZE_SMALL),
    normal(320, 470, Configuration.SCREENLAYOUT_SIZE_NORMAL),
    large(480, 640, Configuration.SCREENLAYOUT_SIZE_LARGE),
    xlarge(720, 960, Configuration.SCREENLAYOUT_SIZE_XLARGE);

    private final int minX;
    private final int minY;
    private final int configValue;

    ScreenSize(int minX, int minY, int configValue) {
      this.minX = minX;
      this.minY = minY;
      this.configValue = configValue;
    }

    private boolean isSmallerThanOrEqualTo(int x, int y) {
      if (y < x) {
        int oldY = y;
        y = x;
        x = oldY;
      }

      return minX <= x && minY <= y;
    }

    static ScreenSize find(int configValue) {
      switch (configValue) {
        case Configuration.SCREENLAYOUT_SIZE_SMALL:
          return small;
        case Configuration.SCREENLAYOUT_SIZE_NORMAL:
          return normal;
        case Configuration.SCREENLAYOUT_SIZE_LARGE:
          return large;
        case Configuration.SCREENLAYOUT_SIZE_XLARGE:
          return xlarge;
        case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
          return null;
        default:
          throw new IllegalArgumentException();
      }
    }

    static ScreenSize match(int x, int y) {
      ScreenSize bestMatch = small;

      for (ScreenSize screenSize : values()) {
        if (screenSize.isSmallerThanOrEqualTo(x, y)) {
          bestMatch = screenSize;
        }
      }

      return bestMatch;
    }
  }

  public static void applyRules(Configuration configuration, DisplayMetrics displayMetrics,
      int apiLevel) {
    Locale locale = getLocale(configuration, apiLevel);

    String language = locale == null ? "" : locale.getLanguage();
    if (language.isEmpty()) {
      language = "en";

      String country = locale == null ? "" : locale.getCountry();
      if (country.isEmpty()) {
        country = "us";
      }

      locale = new Locale(language, country);
      setLocale(apiLevel, configuration, locale);
    }

    if (apiLevel <= ConfigDescription.SDK_JELLY_BEAN &&
        getScreenLayoutLayoutDir(configuration) == Configuration.SCREENLAYOUT_LAYOUTDIR_UNDEFINED) {
      setScreenLayoutLayoutDir(configuration, Configuration.SCREENLAYOUT_LAYOUTDIR_LTR);
    }

    ScreenSize requestedScreenSize = ScreenSize.find(getScreenLayoutSize(configuration));
    if (requestedScreenSize == null) {
      requestedScreenSize = ScreenSize.normal;
    }

    if (configuration.screenWidthDp == 0) {
      configuration.screenWidthDp = requestedScreenSize.minX;
    }

    if (configuration.screenHeightDp == 0) {
      configuration.screenHeightDp = requestedScreenSize.minY;
    }

    if (configuration.smallestScreenWidthDp == 0) {
      configuration.smallestScreenWidthDp =
          Math.min(configuration.screenWidthDp, configuration.screenHeightDp);
    }

    if (getScreenLayoutSize(configuration) == Configuration.SCREENLAYOUT_SIZE_UNDEFINED) {
      ScreenSize screenSize =
          ScreenSize.match(configuration.screenWidthDp, configuration.screenHeightDp);
      setScreenLayoutSize(configuration, screenSize.configValue);
    }

    if (getScreenLayoutLong(configuration) == Configuration.SCREENLAYOUT_LONG_UNDEFINED) {
      setScreenLayoutLong(configuration, Configuration.SCREENLAYOUT_LONG_NO);
    }

    if (getScreenLayoutRound(configuration) == Configuration.SCREENLAYOUT_ROUND_UNDEFINED) {
      setScreenLayoutRound(configuration, Configuration.SCREENLAYOUT_ROUND_NO);
    }

    if (configuration.orientation == Configuration.ORIENTATION_UNDEFINED) {
      configuration.orientation = configuration.screenWidthDp > configuration.screenHeightDp
          ? Configuration.ORIENTATION_LANDSCAPE
          : Configuration.ORIENTATION_PORTRAIT;
    } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        && configuration.screenWidthDp > configuration.screenHeightDp) {
      swapXY(configuration);
    } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        && configuration.screenWidthDp < configuration.screenHeightDp) {
      swapXY(configuration);
    }

    if (getUiModeNight(configuration) == Configuration.UI_MODE_NIGHT_UNDEFINED) {
      setUiModeNight(configuration, Configuration.UI_MODE_NIGHT_NO);
    }

    switch (displayMetrics.densityDpi) {
      case ResTable_config.DENSITY_DPI_ANY:
        throw new IllegalArgumentException("'anydpi' isn't actually a dpi");
      case ResTable_config.DENSITY_DPI_NONE:
        throw new IllegalArgumentException("'nodpi' isn't actually a dpi");
      case ResTable_config.DENSITY_DPI_UNDEFINED:
        // DisplayMetrics.DENSITY_DEFAULT is mdpi
        setDensity(ResTable_config.DENSITY_DPI_MDPI, apiLevel, configuration, displayMetrics);
    }

    if (configuration.touchscreen == Configuration.TOUCHSCREEN_UNDEFINED) {
      configuration.touchscreen = Configuration.TOUCHSCREEN_FINGER;
    }

    if (configuration.keyboardHidden == Configuration.KEYBOARDHIDDEN_UNDEFINED) {
      configuration.keyboardHidden = Configuration.KEYBOARDHIDDEN_SOFT;
    }

    if (configuration.keyboard == Configuration.KEYBOARD_UNDEFINED) {
      configuration.keyboard = Configuration.KEYBOARD_QWERTY;
    }

    if (configuration.navigationHidden == Configuration.NAVIGATIONHIDDEN_UNDEFINED) {
      configuration.navigationHidden = Configuration.NAVIGATIONHIDDEN_YES;
    }

    if (configuration.navigation == Configuration.NAVIGATION_UNDEFINED) {
      configuration.navigation = Configuration.NAVIGATION_NONAV;
    }
  }

  private static void swapXY(Configuration configuration) {
    int oldWidth = configuration.screenWidthDp;
    configuration.screenWidthDp = configuration.screenHeightDp;
    configuration.screenHeightDp = oldWidth;
  }

  private static void setLocale(int apiLevel, Configuration configuration, Locale locale) {
    if (apiLevel >= VERSION_CODES.JELLY_BEAN_MR1) {
      configuration.setLocale(locale);
    } else {
      configuration.locale = locale;
    }
  }

  private static Locale getLocale(Configuration configuration, int apiLevel) {
    Locale locale;
    if (RuntimeEnvironment.getApiLevel() > Build.VERSION_CODES.M) {
      locale = configuration.getLocales().get(0);
    } else {
      locale = configuration.locale;
    }
    return locale;
  }

  private static int getScreenLayoutSize(Configuration configuration) {
    return configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
  }

  private static void setScreenLayoutSize(Configuration configuration, int value) {
    configuration.screenLayout =
        (configuration.screenLayout & ~Configuration.SCREENLAYOUT_SIZE_MASK)
            | value;
  }

  private static int getScreenLayoutLong(Configuration configuration) {
    return configuration.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;
  }

  private static void setScreenLayoutLong(Configuration configuration, int value) {
    configuration.screenLayout =
        (configuration.screenLayout & ~Configuration.SCREENLAYOUT_LONG_MASK)
            | value;
  }

  private static int getScreenLayoutLayoutDir(Configuration configuration) {
    return configuration.screenLayout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK;
  }

  private static void setScreenLayoutLayoutDir(Configuration configuration, int value) {
    configuration.screenLayout =
        (configuration.screenLayout & ~Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
            | value;
  }

  private static int getScreenLayoutRound(Configuration configuration) {
    return configuration.screenLayout & Configuration.SCREENLAYOUT_ROUND_MASK;
  }

  private static void setScreenLayoutRound(Configuration configuration, int value) {
    configuration.screenLayout =
        (configuration.screenLayout & ~Configuration.SCREENLAYOUT_ROUND_MASK)
            | value;
  }

  private static int getUiModeType(Configuration configuration) {
    return configuration.uiMode & Configuration.UI_MODE_TYPE_MASK;
  }

  private static void setUiModeType(Configuration configuration, int value) {
    configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_TYPE_MASK) | value;
  }

  private static int getUiModeNight(Configuration configuration) {
    return configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
  }

  private static void setUiModeNight(Configuration configuration, int value) {
    configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | value;
  }
}
