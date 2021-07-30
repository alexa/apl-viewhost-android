/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.ColorFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Base class for a FilterOperation.
 *
 * Runs on a thread per operation and blocks until the source and destination bitmaps are ready.
 */
public abstract class FilterOperation implements Callable<FilterResult> {
    private static final String TAG = "FilterOperation";

    private final List<Future<FilterResult>> mSourceFutures;
    private final Filters.Filter mFilter;
    private final IBitmapFactory mBitmapFactory;

    /**
     * Constructs a FilterOperation that takes source bitmaps and the filter to apply.
     *
     * @param sourceFutures This is a List of 0, 1, or 2 FilterResults in the following order:
     *                      []
     *                      [source]
     *                      [source, destination]
     * @param filter        the filter to apply
     */
    FilterOperation(List<Future<FilterResult>> sourceFutures, Filters.Filter filter, IBitmapFactory bitmapFactory) {
        mSourceFutures = sourceFutures;
        mFilter = filter;
        mBitmapFactory = bitmapFactory;
    }

    /**
     * Default implementation for creating bitmaps from source and destination needed for filter processing.
     * @return a set of FilterBitmaps for processing.
     * @throws BitmapCreationException if a bitmap failed to be created.
     */
    FilterBitmaps createFilterBitmaps() throws BitmapCreationException {
        return FilterBitmaps.create(null, null, null);
    }

    /**
     * Get's the filter for this operation.
     * @return the filter.
     */
    Filters.Filter getFilter() {
        return mFilter;
    }

    IBitmapFactory getBitmapFactory() {
        return mBitmapFactory;
    }

    /**
     * Gets the source bitmap, blocking the thread until it's ready.
     * @return the source bitmap, or a Transparent bitmap if an exception is thrown waiting.
     */
    @Nullable
    FilterResult getSource() {
        if (mSourceFutures.size() == 0) {
            return null;
        }

        try {
            return mSourceFutures.get(0).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Exception retrieving source.", e);
            return new ColorFilterResult(Color.TRANSPARENT, mBitmapFactory);
        }
    }

    /**
     * Gets the destination bitmap, blocking the thread until it's ready.
     * @return the destination bitmap, or a Transparent bitmap if an exception is thrown waiting.
     */
    @Nullable
    FilterResult getDestination() {
        if (mSourceFutures.size() != 2) {
            return null;
        }

        try {
            return mSourceFutures.get(1).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Exception retrieving destination.", e);
            return new ColorFilterResult(Color.TRANSPARENT, mBitmapFactory);
        }
    }

    /**
     * Class to hold the bitmaps needed for processing. These are deferred until we {@link #call()} this
     * FilterOperation.
     */
    @AutoValue
    static abstract class FilterBitmaps {
        @Nullable
        abstract Bitmap source();
        @Nullable
        abstract Bitmap destination();
        @Nullable
        abstract Bitmap result();

        static FilterBitmaps create(Bitmap source, Bitmap destination, Bitmap result) {
            return new AutoValue_FilterOperation_FilterBitmaps(source, destination, result);
        }
    }
}
