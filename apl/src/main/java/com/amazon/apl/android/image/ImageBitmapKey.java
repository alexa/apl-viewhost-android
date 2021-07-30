/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import androidx.annotation.Nullable;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.bitmap.BitmapKey;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class ImageBitmapKey implements BitmapKey {

    abstract List<String> sources();
    abstract Rect bounds();
    abstract Rect innerBounds();
    abstract ImageScale scale();
    abstract ImageAlign align();
    abstract int overlayColor();
    @Nullable
    abstract Gradient overlayGradient();
    abstract Filters filters();

    public static ImageBitmapKey create(List<String> sources, Rect bounds, Rect innerBounds, ImageScale scale, ImageAlign align, int overlayColor, @Nullable Gradient overlayGradient, Filters filters) {
        return new AutoValue_ImageBitmapKey(sources, bounds, innerBounds, scale, align, overlayColor, overlayGradient, filters);
    }

    public static ImageBitmapKey create(Image image) {
        return create(image.getSources(), image.getBounds(), image.getInnerBounds(), image.getScale(), image.getAlign(), image.getOverlayColor(), image.getOverlayGradient(), image.getFilters());
    }
}
