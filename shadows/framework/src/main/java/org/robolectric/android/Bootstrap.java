package org.robolectric.android;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import com.google.common.annotations.VisibleForTesting;
import org.robolectric.res.Qualifiers;

public class Bootstrap {

  @VisibleForTesting
  public static void applyQualifiers(String qualifiersStr, int apiLevel,
      Configuration configuration, DisplayMetrics displayMetrics) {

    int platformVersion = Qualifiers.getPlatformVersion(qualifiersStr);
    if (platformVersion != -1 && platformVersion != apiLevel) {
      throw new IllegalArgumentException(
          "Cannot specify conflicting platform version in qualifiers: \"" + qualifiersStr + "\"");
    }

    Qualifiers qualifiers = Qualifiers.parse(qualifiersStr);

    DeviceConfig.applyToConfiguration(qualifiers, apiLevel, configuration, displayMetrics);

    DeviceConfig.applyRules(configuration, displayMetrics, apiLevel);
  }

}
