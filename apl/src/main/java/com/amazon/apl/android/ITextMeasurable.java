/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

import com.amazon.apl.enums.Display;

/**
 * Interface for text components which need to measure their dimensions.
 */
public interface ITextMeasurable {
    /***
     * The pixel width of this layout as calculated by
     * {@link #measureTextContent(float, float, RootContext.MeasureMode, float, RootContext.MeasureMode)}
     *
     * @return The raw measured width of this Text layout.
     */
    int getMeasuredWidthPx();

    /***
     * The pixel height of this layout as calculated by
     * {@link #measureTextContent(float, float, RootContext.MeasureMode, float, RootContext.MeasureMode)}
     *
     * @return The raw measured width of this Text layout.
     */
    int getMeasuredHeightPx();

    /**
     * Core calls back into this method when it needs the dimensions of the text based on Font metrics.
     * @param density
     * @param widthPx
     * @param widthMode
     * @param heightPx
     * @param heightMode
     */
    void measureTextContent(
            final float density,
            final float widthPx,
            @NonNull final RootContext.MeasureMode widthMode,
            final float heightPx,
            @NonNull final RootContext.MeasureMode heightMode);

    default boolean shouldSkipMeasurementPass(float widthPx, float heightPx,
                                              RootContext.MeasureMode widthMode, Display display) {
        if (display == Display.kDisplayNone) {
            return true;
        }

        final boolean widthModeExactlyOrAtMost = (widthMode == RootContext.MeasureMode.Exactly ||
                widthMode == RootContext.MeasureMode.AtMost);
        return widthPx == 0 && heightPx == 0 && widthModeExactlyOrAtMost;
    }
}
