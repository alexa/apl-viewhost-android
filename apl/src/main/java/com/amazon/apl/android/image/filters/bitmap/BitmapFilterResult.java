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
public class BitmapFilterResult implements FilterResult {
    protected final Bitmap mBitmap;
    protected final IBitmapFactory mBitmapFactory;

    public BitmapFilterResult(Bitmap bitmap, IBitmapFactory bitmapFactory) {
        mBitmap = bitmap;
        mBitmapFactory = bitmapFactory;
    }

    @Override
    public Size getSize() {
        return Size.create(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    public Bitmap getBitmap(Size size) {
        if (size.equals(getSize())) {
            return mBitmap;
        } else {
            try {
                // Best case is that size is smaller and we return a subset of the bitmap
                if (size.width() <= mBitmap.getWidth() && size.height() <= mBitmap.getHeight()) {
                    return mBitmapFactory.createBitmap(mBitmap, 0, 0, size.width(), size.height(), null,false);
                }

                // Otherwise we create a larger bitmap and copy this bitmap into it.
                Bitmap copy = mBitmapFactory.createBitmap(size.width(), size.height());
                Canvas canvas = new Canvas(copy);
                final Rect rect = new Rect(0, 0, Math.min(mBitmap.getWidth(), size.width()), Math.min(mBitmap.getHeight(), size.height()));
                canvas.drawBitmap(mBitmap, rect, rect, null);
                return copy;
            } catch (BitmapCreationException e) {
                return mBitmap;
            }
        }
    }
}
