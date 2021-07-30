/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.primitive.Gradient;

/**
 * Class representing a Gradient bitmap.
 */
public class GradientFilterResult implements FilterResult {
    private final Gradient mGradient;
    private final IBitmapFactory mBitmapFactory;

    public GradientFilterResult(Gradient gradient, IBitmapFactory bitmapFactory) {
        mGradient = gradient;
        mBitmapFactory = bitmapFactory;
    }

    public Gradient getGradient() {
        return mGradient;
    }

    @Override
    public Size getSize() {
        return Size.ZERO;
    }

    // TODO consider not creating a bitmap here and instead provide a function to retrieve the correct
    //  color value in a pixel array. This would save memory when using a Gradient or Color when performing
    //  Blend.
    @Override
    public Bitmap getBitmap(Size size) {
        try {
            Bitmap bitmap = mBitmapFactory.createBitmap(size.width(), size.height());
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(mGradient.getShader(size.width(), size.height()));
            Canvas canvas = new Canvas(bitmap);
            canvas.drawRect(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), paint);
            return bitmap;
        } catch (BitmapCreationException e) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }
}
