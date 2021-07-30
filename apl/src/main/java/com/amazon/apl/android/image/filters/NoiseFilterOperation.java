/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.NoiseFilterKind;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Apply a noise filter with params: kind, sigma and useColor.
 */
public class NoiseFilterOperation extends FilterOperation {
    private static final String TAG = "NoiseFilterOperation";
    private static final int DEFAULT_NOISE_SEED = 42;

    NoiseFilterOperation(List<Future<FilterResult>> sourceBitmaps, Filters.Filter filter, IBitmapFactory bitmapFactory) {
        super(sourceBitmaps, filter, bitmapFactory);
    }

    @Override
    FilterBitmaps createFilterBitmaps() {
        FilterResult source = getSource();
        if (source == null || !source.isBitmap()) {
            throw new IllegalArgumentException(TAG + ": Source must be a bitmap.");
        }

        Bitmap sourceBitmap = source.getBitmap();

        // TODO this modifies the source bitmap which means that if another filter uses the same bitmap
        //  as this one there is unexpected behavior. Unfortunately, it is expensive to have this use a
        //  copy so for now we use the same one.
        //  return FilterBitmaps.create(sourceBitmap, null, Objects.requireNonNull(sourceBitmap.copy(sourceBitmap.getConfig(), true)));
        return FilterBitmaps.create(sourceBitmap, null, sourceBitmap);
    }

    @Override
    public FilterResult call() {
        FilterBitmaps filterBitmaps = createFilterBitmaps();
        Filters.Filter filter = getFilter();

        setNoiseSeed(DEFAULT_NOISE_SEED);
        noiseFilter(filterBitmaps.result(), Math.round(filter.noiseSigma()), filter.noiseUseColor(), filter.noiseKind() == NoiseFilterKind.kFilterNoiseKindUniform);

        return new BitmapFilterResult(filterBitmaps.result(), getBitmapFactory());
    }

    @VisibleForTesting
    void setNoiseSeed(int noiseSeed) {
        nativeSetNoiseSeed(noiseSeed);
    }

    @VisibleForTesting
    void noiseFilter(Bitmap in, int sigma, boolean useColor, boolean isUniform) {
        nativeNoiseFilter(in, sigma, useColor, isUniform);
    }

    private native void nativeNoiseFilter(Bitmap in, int sigma, boolean useColor, boolean isUniform);
    private native void nativeSetNoiseSeed(int noiseSeed);
}