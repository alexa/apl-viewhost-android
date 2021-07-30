/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;

import android.graphics.RectF;
import androidx.annotation.Nullable;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.bitmap.BitmapKey;

import java.util.Objects;

/**
 * Shadow Bitmap's Key manager based on its shadow properties.
 */
class ShadowBitmapKey implements BitmapKey {
    int height;
    int width;
    int offX;
    int offY;
    int color;
    int blurRadius;
    private final float[] cornerRadius;

    ShadowBitmapKey(final Component component) {
        final RectF rectF = component.getShadowRect();
        height = (int) rectF.height();
        width = (int) rectF.width();
        offX = component.getShadowOffsetHorizontal();
        offY = component.getShadowOffsetVertical();
        color = component.getShadowColor();
        blurRadius = component.getShadowRadius();
        cornerRadius = component.getShadowCornerRadius();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ShadowBitmapKey)) {
            return false;
        }
        ShadowBitmapKey key = (ShadowBitmapKey) obj;
        return height == key.height
                && width == key.width
                && offX == key.offX
                && offY == key.offY
                && color == key.color
                && blurRadius == key.blurRadius
                && Math.round(cornerRadius[0]) == Math.round(key.cornerRadius[0])
                && Math.round(cornerRadius[1]) == Math.round(key.cornerRadius[1])
                && Math.round(cornerRadius[2]) == Math.round(key.cornerRadius[2])
                && Math.round(cornerRadius[3]) == Math.round(key.cornerRadius[3]);
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, width, offX, offY, color, blurRadius,
                cornerRadius[0], cornerRadius[1], cornerRadius[2], cornerRadius[3]);
    }
}