/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.primitive.Radii;

/**
 * GradientDrawable supporting APL properties.
 */
public class APLGradientDrawable extends GradientDrawable {

    private int mBorderWidth = 0;
    private int mBorderColor = Color.TRANSPARENT;
    @Nullable
    private Radii mRadii = null;
    private int lastSetColor = Color.TRANSPARENT;

    @Override
    public void setStroke(int borderWidth, int color) {
        mBorderWidth = borderWidth;
        mBorderColor = color;
        super.setStroke(borderWidth, color);
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setCornerRadii(@NonNull Radii radii) {
        mRadii = radii;
        setCornerRadii(mRadii.toFloatArray());
    }

    @Nullable
    public Radii getRadii() {
        return mRadii;
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        this.lastSetColor = color;
    }

    @VisibleForTesting
    public int getDefaultColor() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // NOTE: GradientDrawable:getColor is only available on Android 24+
            return super.getColor().getDefaultColor();
        } else {
            return this.lastSetColor;
        }
    }
}
