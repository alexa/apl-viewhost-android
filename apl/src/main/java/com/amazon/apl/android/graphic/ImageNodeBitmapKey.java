/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.BitmapKey;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.sgcontent.filters.Filter;
import com.google.auto.value.AutoValue;


/**
 * A bitmap key for Filter processing results.
 */
@AutoValue
public abstract class ImageNodeBitmapKey implements BitmapKey {

    public abstract Filter filter();

    @Nullable
    public abstract Rect sourceRegion();

    @Nullable
    public abstract Size targetSize();

    public static ImageNodeBitmapKey create(Filter filter, Rect sourceRegion, Size targetSize) {
        return new AutoValue_ImageNodeBitmapKey(filter, sourceRegion, targetSize);
    }
}