/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import com.amazon.apl.android.bitmap.BitmapKey;
import com.google.auto.value.AutoValue;

/**
 * A bitmap key for Filter bitmaps in AVG
 */
@AutoValue
public abstract class FilterBitmapKey implements BitmapKey {

    abstract int width();
    abstract int height();
    abstract int groupId();

    public static FilterBitmapKey create(int width, int height, int groupId) {
        return new AutoValue_FilterBitmapKey(width, height, groupId);
    }
}