/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import androidx.annotation.Nullable;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.bitmap.BitmapKey;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.primitive.UrlRequests;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class ProcessedImageBitmapKey implements BitmapKey {

    abstract List<UrlRequests.UrlRequest> sources();
    abstract Rect bounds();
    abstract Filters filters();

    public static ProcessedImageBitmapKey create(List<UrlRequests.UrlRequest> sources, Rect bounds, Filters filters) {
        return new AutoValue_ProcessedImageBitmapKey(sources, bounds, filters);
    }

    @Nullable
    public static ProcessedImageBitmapKey create(Image image) {
        return create(image.getSourceRequests(), image.getBounds(), image.getFilters());
    }
}
