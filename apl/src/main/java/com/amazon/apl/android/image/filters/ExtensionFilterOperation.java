/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.ExtensionFilterParameters;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.List;
import java.util.concurrent.Future;

/**
 * FilterOperation for an extension filter.
 */
public class ExtensionFilterOperation extends FilterOperation {
    private static final String TAG = "ExtensionFilterOperation";

    private final IExtensionImageFilterCallback mCallback;

    ExtensionFilterOperation(List<Future<FilterResult>> sourceBitmaps, Filters.Filter filter, IBitmapFactory bitmapFactory, IExtensionImageFilterCallback callback) {
        super(sourceBitmaps, filter, bitmapFactory);
        mCallback = callback;
    }

    @Override
    FilterBitmaps createFilterBitmaps() {
        FilterResult source = getSource();
        FilterResult destination = getDestination();
        // We require either both bitmaps to be null or at least one to be a proper bitmap.
        if ((source == null && destination != null && !destination.isBitmap()) ||
                (destination == null && source != null && !source.isBitmap()) ||
                (source != null && destination != null && !source.isBitmap() && !destination.isBitmap())) {
            throw new IllegalArgumentException(TAG + ": At least one filter must be a bitmap or both are null.");
        }

        // Following rules are applied in what is passed to the callback:
        //
        // 1. If both source and destination are null, then null arguments are passed.
        // 2. If either source or destination is null, then the expectation is the other is a bitmap.
        // 3. If one of source or destination is not a bitmap (i.e. Color/Gradient), then a Bitmap is created
        //    and passed with the other argument.
        // 4. If both source and destination are bitmaps, then they're passed in without any modifications.
        if (source == null && destination == null) {
            return FilterBitmaps.create(null, null, null);
        } else if (source == null) {
            return FilterBitmaps.create(null, destination.getBitmap(), null);
        } else if (destination == null) {
            return FilterBitmaps.create(source.getBitmap(), null, null);
        } else {
            Bitmap sourceBitmap;
            Bitmap destinationBitmap;
            if (source.isBitmap() && !destination.isBitmap()) {
                sourceBitmap = source.getBitmap();
                destinationBitmap = destination.getBitmap(source.getSize());
            } else if (destination.isBitmap() && !source.isBitmap()) {
                sourceBitmap = source.getBitmap(destination.getSize());
                destinationBitmap = destination.getBitmap();
            } else {
                sourceBitmap = source.getBitmap();
                destinationBitmap = destination.getBitmap();
            }

            return FilterBitmaps.create(sourceBitmap, destinationBitmap, null);
        }
    }

    @Override
    public FilterResult call() {
        final FilterBitmaps filterBitmaps = createFilterBitmaps();
        final Filters.Filter filter = getFilter();
        final ExtensionFilterParameters parameters = ExtensionFilterParameters.create(filter.extensionURI(), filter.name(), filter.source(), filter.destination(), filter.extensionParams());
        final Bitmap result = mCallback.processImage(filterBitmaps.source(), filterBitmaps.destination(), parameters);
        return new BitmapFilterResult(result, getBitmapFactory());
    }
}
