/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scaling;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;

import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;
import com.google.auto.value.AutoValue;

import java.util.Locale;

/**
 * Class containing device specific information as well as {@link Scaling} information.
 */
@AutoValue
public abstract class ViewportMetrics {
    /** Device viewport width in px */
    public abstract int width();

    /** Device viewport minWidth in px */
    public abstract int minWidth();

    /** Device viewport maxWidth in px */
    public abstract int maxWidth();

    /** Device viewport height in px */
    public abstract int height();

    /** Device viewport minHeight in px */
    public abstract int minHeight();

    /** Device viewport maxHeight in px */
    public abstract int maxHeight();

    /** Device dots per inch */
    public abstract int dpi();

    /** Device screen shape */
    public abstract ScreenShape shape();

    /** Device theme */
    public abstract String theme();

    /** Viewing mode */
    public abstract ViewportMode mode();

    /** Scaling profile */
    public abstract Scaling scaling();
    /**
     * Derived value.
     *
     * Scaling factor for the Density Independent Pixel unit.
     * Equivalent to densityDPI / 160
     */
    public float density() {
        return dpi() / DisplayMetrics.DENSITY_DEFAULT;
    }

    /**
     * @return Builder for ViewportMetrics
     */
    public static Builder builder() {
        return new AutoValue_ViewportMetrics.Builder().
                scaling(new Scaling()).minHeight(0).maxHeight(0).minWidth(0).maxWidth(0);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder width(int width);
        public abstract Builder minWidth(int minWidth);
        public abstract Builder maxWidth(int maxWidth);
        public abstract Builder height(int height);
        public abstract Builder minHeight(int minHeight);
        public abstract Builder maxHeight(int maxHeight);
        public abstract Builder dpi(int dpi);
        public abstract Builder shape(ScreenShape shape);
        public abstract Builder theme(String theme);
        public abstract Builder mode(ViewportMode mode);
        public abstract Builder scaling(Scaling scaling);
        public abstract ViewportMetrics build();
    }
}
