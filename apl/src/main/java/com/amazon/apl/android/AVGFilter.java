/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

/**
 * An interface responsible for applying filters to AVG objects
 */
public interface AVGFilter {

    /**
     * Applies the filter effect on the AVG object represented by the given bitmap
     *
     * @param bitmap the given bitmap containing the AVG object and any previous filters applied
     * @param xScale the horizontal scale
     * @param yScale the vertical scale
     */
    void apply(@NonNull Bitmap bitmap, float xScale, float yScale);
}
