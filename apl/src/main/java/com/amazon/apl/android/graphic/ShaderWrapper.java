/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;

import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientUnits;

/**
 * Abstract class for managing the common properties between the AVG Gradient types.
 * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#common
 */
public abstract class ShaderWrapper {
    private final int[] mColorRange;
    private final GradientUnits mGradientUnits;
    private final float[] mInputRange;
    private final Matrix mTransform;
    private final Shader.TileMode mTileMode;

    ShaderWrapper(final Gradient graphicGradient, final Matrix transform) {
        mColorRange = graphicGradient.getColorRange();
        mGradientUnits = graphicGradient.getUnits();
        mInputRange = graphicGradient.getInputRange();
        mTileMode = getTileMode(graphicGradient.getSpreadMethod());
        mTransform = transform;
    }

    abstract Shader getShader();

    int[] getColorRange() {
        return mColorRange;
    }

    float[] getInputRange() {
        return mInputRange;
    }

    Shader.TileMode getTileMode() {
        return mTileMode;
    }

    Matrix getTransform() {
        return mTransform;
    }

    /**
     * Get the x-coordinate w.r.t. the left position of the graphic bounding box.
     * @param bounds
     * @param width
     * @return - x-coordinate
     */
    float getAbsoluteXCoordinate(final Rect bounds, final float width) {
        return bounds.left + getAbsoluteCoordinate(bounds.width(), width);
    }

    /**
     * Get the y-coordinate w.r.t. the top position of the graphic bounding box.
     * @param bounds
     * @param height
     * @return - y-coordinate
     */
    float getAbsoluteYCoordinate(final Rect bounds, final float height) {
        return bounds.top + getAbsoluteCoordinate(bounds.height(), height);
    }

    /**
     * Get the {@link Shader.TileMode} based on the SpreadMethod
     * Defaults to PAD.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#spreadmethod
     * @param gradientSpreadMethod
     * @return - {@link Shader.TileMode}
     */
    Shader.TileMode getTileMode(final GradientSpreadMethod gradientSpreadMethod) {
        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        switch(gradientSpreadMethod) {
            case PAD:
                tileMode = Shader.TileMode.CLAMP;
                break;
            case REFLECT:
                tileMode = Shader.TileMode.MIRROR;
                break;
            case REPEAT:
                tileMode = Shader.TileMode.REPEAT;
                break;
        }
        return tileMode;
    }

    float getAbsoluteCoordinate(final float bounds, final float dimension) {
        switch(mGradientUnits) {
            case kGradientUnitsUserSpace:
                return dimension;
            case kGradientUnitsBoundingBox:
            default:
                return bounds * dimension;
        }
    }
}
