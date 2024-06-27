/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;

/**
 * Class representing an actual bitmap.
 */
public class BitmapRegionFilterResult implements FilterResult {
    protected final Bitmap mBitmap;
    protected final IBitmapFactory mBitmapFactory;
    private final Rect mDecodeRegion;
    private final Rect mRequestedSourceRegion;

    public BitmapRegionFilterResult(Bitmap bitmap, IBitmapFactory bitmapFactory, Rect requestedSourceRegion, Rect decodeRegion) {
        mBitmap = bitmap;
        mBitmapFactory = bitmapFactory;
        mRequestedSourceRegion = requestedSourceRegion;
        mDecodeRegion = decodeRegion;
    }

    @Override
    public Size getSize() {
        return Size.create(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    public Bitmap getBitmap(Size size) {
        if (size.equals(getSize()) && mRequestedSourceRegion.equals(mDecodeRegion)) {
            return mBitmap;
        } else {
            int translatedLeft = mRequestedSourceRegion.left - mDecodeRegion.left;
            int translatedTop = mRequestedSourceRegion.top - mDecodeRegion.top;
            float widthSampleRate = (float)mBitmap.getWidth() / mDecodeRegion.width();
            int regionWidthAtDecodedSampleRate = Math.round(mRequestedSourceRegion.width() * widthSampleRate);
            float heightSampleRate = (float)mBitmap.getHeight() / mDecodeRegion.height();
            int regionHeightAtDecodedSampleRate = Math.round(mRequestedSourceRegion.height() * heightSampleRate);

            Rect translatedSourceRegion = new Rect(translatedLeft, translatedTop, translatedLeft + regionWidthAtDecodedSampleRate, translatedTop + regionHeightAtDecodedSampleRate);
            try {
                Bitmap copy = mBitmapFactory.createBitmap(size.width(), size.height());
                Canvas canvas = new Canvas(copy);
                final Rect targetRect = new Rect(0, 0, size.width(), size.height());
                canvas.drawBitmap(mBitmap, translatedSourceRegion, targetRect, null);
                return copy;
            } catch (BitmapCreationException e) {
                return mBitmap;
            }
        }
    }
}
