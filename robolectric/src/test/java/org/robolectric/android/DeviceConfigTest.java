package org.robolectric.android;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.res.Qualifiers;

@RunWith(RobolectricTestRunner.class)
public class DeviceConfigTest {

  private Configuration configuration;
  private DisplayMetrics displayMetrics;
  private int apiLevel;

  @Before
  public void setUp() throws Exception {
    configuration = new Configuration();
    displayMetrics = new DisplayMetrics();
    apiLevel = RuntimeEnvironment.getApiLevel();
  }

  @Test
  public void applyToConfiguration() throws Exception {
    applyQualifiers("en-rUS-w400dp-h800dp-notround");
    assertThat(asQualifierString())
        .isEqualTo("en-rUS-ldltr-w400dp-h800dp-notround");
  }

  @Test
  public void applyToConfiguration_isCumulative() throws Exception {
    applyQualifiers("en-rUS-ldltr-sw400dp-w400dp-h800dp-normal-notlong-notround-port-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");
    assertThat(asQualifierString())
        .isEqualTo("en-rUS-ldltr-sw400dp-w400dp-h800dp-normal-notlong-notround-port-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");

    applyQualifiers("fr-land");
    assertThat(asQualifierString())
        .isEqualTo("fr-ldltr-sw400dp-w400dp-h800dp-normal-notlong-notround-land-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");

    applyQualifiers("w500dp-large-television-night-xxhdpi-notouch-keyshidden");
    assertThat(asQualifierString())
        .isEqualTo("fr-ldltr-sw400dp-w500dp-h800dp-large-notlong-notround-land-television-night-xxhdpi-notouch-keyshidden-qwerty-navhidden-nonav");

    applyQualifiers("long");
    assertThat(asQualifierString())
        .isEqualTo("fr-ldltr-sw400dp-w500dp-h800dp-large-long-notround-land-television-night-xxhdpi-notouch-keyshidden-qwerty-navhidden-nonav");

    applyQualifiers("round");
    assertThat(asQualifierString())
        .isEqualTo("fr-ldltr-sw400dp-w500dp-h800dp-large-long-round-land-television-night-xxhdpi-notouch-keyshidden-qwerty-navhidden-nonav");
  }

  @Test
  public void applyRules_defaults() throws Exception {
    DeviceConfig.applyRules(configuration, displayMetrics, apiLevel);

    assertThat(asQualifierString())
        .isEqualTo("en-rUS-ldltr-sw320dp-w320dp-h470dp-normal-notlong-notround-port-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");
  }

  @Test
  public void applyRules_rtlScript() throws Exception {
    applyQualifiers("he");
    DeviceConfig.applyRules(configuration, displayMetrics, apiLevel);

    assertThat(asQualifierString())
        .isEqualTo("iw-ldrtl-sw320dp-w320dp-h470dp-normal-notlong-notround-port-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");
  }

  @Test
  public void applyRules_heightWidth() throws Exception {
    applyQualifiers("w800dp-h400dp");
    DeviceConfig.applyRules(configuration, displayMetrics, apiLevel);

    assertThat(asQualifierString())
        .isEqualTo("en-rUS-ldltr-sw400dp-w800dp-h400dp-normal-notlong-notround-land-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");
  }

  @Test
  public void applyRules_heightWidthOrientation() throws Exception {
    applyQualifiers("w800dp-h400dp-port");
    DeviceConfig.applyRules(configuration, displayMetrics, apiLevel);

    assertThat(asQualifierString())
        .isEqualTo("en-rUS-ldltr-sw400dp-w400dp-h800dp-normal-notlong-notround-port-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");
  }

  @Test
  public void applyRules_size() throws Exception {
    applyQualifiers("large-land");
    DeviceConfig.applyRules(configuration, displayMetrics, apiLevel);

    assertThat(asQualifierString())
        .isEqualTo("en-rUS-ldltr-sw480dp-w640dp-h480dp-large-notlong-notround-land-notnight-mdpi-finger-keyssoft-qwerty-navhidden-nonav");
  }

  //////////////////////////

  private void applyQualifiers(String qualifiers) {
    DeviceConfig.applyToConfiguration(Qualifiers.parse(
        qualifiers),
        apiLevel, configuration, displayMetrics);
  }

  private String asQualifierString() {
    return ConfigurationV25.resourceQualifierString(configuration, displayMetrics, false);
  }
}
