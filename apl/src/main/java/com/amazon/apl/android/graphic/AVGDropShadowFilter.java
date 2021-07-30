/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import androidx.annotation.NonNull;

import com.amazon.apl.android.AVGFilter;

/**
 * An AVG filter which adds a DropShadow effect on AVG objects
 */
public class AVGDropShadowFilter implements AVGFilter {

    private final int mColor;
    private final float mRadius;
    private final float mHorizontalOffset;
    private final float mVerticalOffset;

    public AVGDropShadowFilter(int color, float radius, float horizontalOffset, float verticalOffset) {
        mColor = color;
        mRadius = radius;
        mHorizontalOffset = horizontalOffset;
        mVerticalOffset = verticalOffset;
    }


    /**
     * Applies the DropShadow filter effect on the AVG object represented by the given bitmap
     *
     * @param bitmap the given bitmap containing the AVG object and any previous filters applied
     * @param xScale the horizontal scale
     * @param yScale the vertical scale
     * @return a bitmap with the applied DropShadow filter effect
     */
    @Override
    public void apply(@NonNull Bitmap bitmap, float xScale, float yScale) {
        float scaledRadius = mRadius * Math.min(xScale, yScale);
        if(scaledRadius < 0) return;

        final Canvas canvas = new Canvas(bitmap);
        final Bitmap alphaBitmap = bitmap.extractAlpha();
        final Paint shadowPaint = new Paint();

        shadowPaint.setColor(mColor);
        if (scaledRadius != 0) {
            shadowPaint.setMaskFilter(new BlurMaskFilter(scaledRadius, BlurMaskFilter.Blur.NORMAL));
        }
        shadowPaint.setColorFilter(new PorterDuffColorFilter(shadowPaint.getColor(), PorterDuff.Mode.MULTIPLY));
        shadowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

        canvas.translate(mHorizontalOffset * xScale, mVerticalOffset * yScale);
        canvas.drawBitmap(alphaBitmap, new Matrix(), shadowPaint);

        alphaBitmap.recycle();
    }
}
