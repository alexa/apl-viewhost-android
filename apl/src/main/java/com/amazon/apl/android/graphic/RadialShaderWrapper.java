/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Matrix;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.primitive.Gradient;

/**
 * Wrapper class for creating a {@link RadialGradient} object. Used in APL 1.6 and below.
 */
public class RadialShaderWrapper extends ShaderWrapper {
    private static final String TAG = "RadialShaderWrapper";
    /**
     * The coordinates of the center of the radial gradient.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#radial
     */
    private final float mCenterX;
    private final float mCenterY;
    private final float mRadius;

    /**
     * Get the calculated absolute center x-coordinate
     * @return the x-coordinate
     */
    float getCenterX() {
        return mCenterX;
    }

    /**
     * Get the calculated absolute center y-coordinate
     * @return the y-coordinate
     */
    float getCenterY() {
        return mCenterY;
    }

    /**
     * Get the calculated absolute radius value
     * @return the radius value
     */
    float getRadius() {
        return mRadius;
    }

    RadialShaderWrapper(final Gradient graphicGradient, final Rect bounds, final Matrix transform) {
        super(graphicGradient, transform);
        mCenterX = getAbsoluteXCoordinate(bounds, graphicGradient.getCenterX());
        mCenterY = getAbsoluteYCoordinate(bounds, graphicGradient.getCenterY());
        mRadius = getScaledRadius(bounds, graphicGradient.getRadius());
    }

    /**
     * Get the {@link RadialGradient} object.
     * @return - {@link RadialGradient}
     */
    @Override
    @Nullable
    Shader getShader() {
        if (mRadius <= 0) {
            Log.w(TAG, "Cannot create a RadialGradient shader with a non-positive radius");
            return null;
        }
        Shader radialGradient = new RadialGradient(getCenterX(), getCenterY(), getRadius(), getColorRange(), getInputRange(), getTileMode());
        radialGradient.setLocalMatrix(getTransform());
        return radialGradient;
    }

    /**
     * Get the scaled radius.
     * @param bounds
     * @param unitSquarePercentage
     * @return - radius of the radial gradient elipse.
     */
    float getScaledRadius(final Rect bounds, final float unitSquarePercentage) {
        final float width = bounds.width();
        final float height = bounds.height();
        return ((float) Math.sqrt((width * width) + (height * height)) / 2f) * unitSquarePercentage;
    }
}
