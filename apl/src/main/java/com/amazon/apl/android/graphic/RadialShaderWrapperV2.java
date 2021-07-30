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
import com.amazon.apl.enums.GradientUnits;

/**
 * Wrapper class for creating a {@link RadialGradient} object. Used in APL 1.7 and above. Fixes several issues with {@link RadialShaderWrapper}
 */
public class RadialShaderWrapperV2 extends RadialShaderWrapper {
    private final float mRadius;
    private final Matrix mTransform;

    RadialShaderWrapperV2(final Gradient graphicGradient, final Rect bounds, final Matrix transform) {
        super(graphicGradient, bounds, transform);
        mRadius = getAbsoluteRadius(graphicGradient.getUnits(), bounds, graphicGradient.getRadius());
        mTransform = getScaledTransform(graphicGradient.getUnits(), bounds);
    }

    /**
     * Get the calculated absolute radius value.
     * @return the radius value
     */
    @Override
    float getRadius() {
        return mRadius;
    }

    /**
     * Get the calculated transform matrix.
     * @return the transform matrix
     */
    @Override
    Matrix getTransform() {
        return mTransform;
    }

    private float getAbsoluteRadius(final GradientUnits gradientUnits, final Rect bounds, final float radius) {
        if (gradientUnits.equals(GradientUnits.kGradientUnitsUserSpace)) {
            return radius;
        }

        // in BoundingBox units the radius is a percentage of the major axis
        float majorAxisLength = Math.max(bounds.width(), bounds.height());
        return radius * majorAxisLength;
    }

    private Matrix getScaledTransform(final GradientUnits gradientUnits, final Rect bounds) {
        Matrix scaledTransform = new Matrix(super.getTransform());

        if (gradientUnits.equals(GradientUnits.kGradientUnitsBoundingBox)) {
            float majorAxisLength = Math.max(bounds.width(), bounds.height());

            // in BoundingBox mode the gradient should stretch if the bounding box is not square
            float scaleX = bounds.width() / majorAxisLength;
            float scaleY = bounds.height() / majorAxisLength;
            scaledTransform.preScale(scaleX, scaleY, getCenterX(), getCenterY());
        }

       return scaledTransform;
    }
}
