/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.configuration;

import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.ScreenMode;
import com.amazon.apl.enums.ViewportMode;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

/**
 * APL Configuration change class to notify APL Core Engine of changes in configuration such as orientation change.
 * Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#onconfigchange
 */
@AutoValue
public abstract class ConfigurationChange {
    /** Viewport Characteristics derived from ViewportMetrics */
    public abstract int width();
    public abstract int minWidth();
    public abstract int maxWidth();
    public abstract int height();
    public abstract int minHeight();
    public abstract int maxHeight();
    public abstract String theme();
    public abstract ViewportMode mode();
    /** RootConfig parameters */
    public abstract ScreenMode screenMode();
    public abstract boolean screenReaderEnabled();
    public abstract float fontScale();
    public abstract boolean disallowVideo();
    public abstract ImmutableMap<String, Object> environmentValues();

    public static ConfigurationChange.Builder create(ViewportMetrics viewportMetrics, RootConfig rootConfig) {
        return new AutoValue_ConfigurationChange.Builder().
                width(viewportMetrics.width()).
                maxWidth(viewportMetrics.maxWidth()).
                minWidth(viewportMetrics.minWidth()).
                height(viewportMetrics.height()).
                maxHeight(viewportMetrics.maxHeight()).
                minHeight(viewportMetrics.minHeight()).
                theme(viewportMetrics.theme()).
                mode(viewportMetrics.mode()).
                screenMode(rootConfig.getScreenModeEnumerated()).
                screenReaderEnabled(rootConfig.getScreenReader()).
                fontScale(rootConfig.getFontScale()).
                disallowVideo((boolean) rootConfig.getProperty(RootProperty.kDisallowVideo));
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder width(int width);
        public abstract Builder minWidth(int minWidth);
        public abstract Builder maxWidth(int maxWidth);
        public abstract Builder height(int height);
        public abstract Builder minHeight(int minHeight);
        public abstract Builder maxHeight(int maxHeight);
        public abstract Builder theme(String theme);
        public abstract Builder mode(ViewportMode mode);
        public abstract Builder screenMode(ScreenMode screenMode);
        public abstract Builder screenReaderEnabled(boolean isScreenReaderEnabled);
        public abstract Builder fontScale(float fontScale);
        public abstract Builder disallowVideo(boolean isVideoDisallowed);
        abstract ImmutableMap.Builder<String, Object> environmentValuesBuilder();

        public Builder environmentValue(String key, Object value) {
            environmentValuesBuilder().put(key, value);
            return this;
        }
        public abstract ConfigurationChange build();
    }
}
