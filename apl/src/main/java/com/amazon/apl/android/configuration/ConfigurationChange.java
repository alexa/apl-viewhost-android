/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.configuration;

import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenMode;
import com.amazon.apl.enums.ViewportMode;
import com.google.auto.value.AutoValue;

/**
 * APL Configuration change class to notify APL Core Engine of changes in configuration such as orientation change.
 * Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#onconfigchange
 */
@AutoValue
public abstract class ConfigurationChange {
    /** Viewport Characteristics derived from ViewportMetrics */
    public abstract int width();
    public abstract int height();
    public abstract String theme();
    public abstract ViewportMode mode();
    /** RootConfig parameters */
    public abstract ScreenMode screenMode();
    public abstract boolean screenReaderEnabled();
    public abstract float fontScale();

    public static ConfigurationChange.Builder create(ViewportMetrics viewportMetrics, RootConfig rootConfig) {
        return new AutoValue_ConfigurationChange.Builder().
                width(viewportMetrics.width()).
                height(viewportMetrics.height()).
                theme(viewportMetrics.theme()).
                mode(viewportMetrics.mode()).
                screenMode(rootConfig.getScreenModeEnumerated()).
                screenReaderEnabled(rootConfig.getScreenReader()).
                fontScale(rootConfig.getFontScale());
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder width(int width);
        public abstract Builder height(int height);
        public abstract Builder theme(String theme);
        public abstract Builder mode(ViewportMode mode);
        public abstract Builder screenMode(ScreenMode screenMode);
        public abstract Builder screenReaderEnabled(boolean isScreenReaderEnabled);
        public abstract Builder fontScale(float fontScale);
        public abstract ConfigurationChange build();
    }
}
