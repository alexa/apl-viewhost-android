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
import com.amazon.apl.android.image.filters.SolidFilterOperation;

/**
 * Class representing the result of the {@link SolidFilterOperation}.
 */
public class PaintFilterResult implements FilterResult {
    private final SolidFilterOperation.PaintProvider mPaintProvider;
    private final IBitmapFactory mBitmapFactory;

    public PaintFilterResult(SolidFilterOperation.PaintProvider paintProvider, IBitmapFactory bitmapFactory) {
        mPaintProvider = paintProvider;
        mBitmapFactory = bitmapFactory;
    }

    @Override
    public Size getSize() {
        return Size.ZERO;
    }

    @Override
    public Bitmap getBitmap(Size size) {
        try {
            Bitmap bitmap = mBitmapFactory.createBitmap(size.width(), size.height());
            Canvas canvas = new Canvas(bitmap);
            Rect drawArea = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawRect(drawArea, mPaintProvider.getPaint(drawArea));
            return bitmap;
        } catch (BitmapCreationException e) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }
}
