/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
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

    /**
     * A bitmask that defines the bits android uses for alpha.
     */
    private static final int ALPHA_BITMASK = 0xFF000000;

    // As per official android guidelines, Paints should be reused.
    final Paint mShadowPaint = new Paint();
    final Paint mTransferPaint = new Paint();

    private final int mColor;
    private final float mRadius;
    private final float mHorizontalOffset;
    private final float mVerticalOffset;

    private float mLastScaledRadius = -1;

    public AVGDropShadowFilter(int color, float radius, float horizontalOffset, float verticalOffset) {
        mColor = color;
        mRadius = radius;
        mHorizontalOffset = horizontalOffset;
        mVerticalOffset = verticalOffset;

        // Fix the alpha component, which will be calculated in the PorterDuffColorFilter
        mTransferPaint.setColor(mColor | ALPHA_BITMASK);
        // Fix the color of src, but take the alpha component from the src color
        mTransferPaint.setColorFilter(new PorterDuffColorFilter(~ALPHA_BITMASK | mColor, PorterDuff.Mode.DST_IN));
        mTransferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
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

        if (mLastScaledRadius != scaledRadius) {
            if (scaledRadius != 0) {
                mShadowPaint.setMaskFilter(new BlurMaskFilter(scaledRadius, BlurMaskFilter.Blur.NORMAL));
            } else {
                // Clear the mask filter.
                mShadowPaint.setMaskFilter(null);
            }
            mLastScaledRadius = scaledRadius;
        }

        int[] offsetXY = new int[2]; // will be filled out by extractAlpha
        final Bitmap alphaBlurredBitmap = bitmap.extractAlpha(mShadowPaint, offsetXY);

        canvas.translate(mHorizontalOffset * xScale, mVerticalOffset * yScale);

        canvas.drawBitmap(alphaBlurredBitmap, offsetXY[0], offsetXY[1], mTransferPaint);
        alphaBlurredBitmap.recycle();
    }
}
