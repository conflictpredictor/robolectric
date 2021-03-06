package org.robolectric.shadows;

import android.preference.Preference;
import android.preference.PreferenceManager;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.internal.Shadow;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Shadow for {@link android.preference.Preference}.
 */
@Implements(Preference.class)
public class ShadowPreference {
  @RealObject private Preference realPreference;

  public void callOnAttachedToHierarchy(PreferenceManager preferenceManager) {
    Shadow.directlyOn(realPreference, Preference.class, "onAttachedToHierarchy",
        ClassParameter.from(PreferenceManager.class, preferenceManager));
  }

  public boolean click() {
    return realPreference.getOnPreferenceClickListener().onPreferenceClick(realPreference);
  }
}
