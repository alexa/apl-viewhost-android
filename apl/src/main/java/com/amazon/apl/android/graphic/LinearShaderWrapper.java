/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;

import com.amazon.apl.android.primitive.Gradient;

/**
 * Wrapper class for creating a {@link LinearGradient} object.
 */
public class LinearShaderWrapper extends ShaderWrapper {
    /**
     * The coordinates of the gradient line segment.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#linear
     */
    private final float mX1;
    private final float mY1;
    private final float mX2;
    private final float mY2;

    float getX1() {
        return mX1;
    }

    float getY1() {
        return mY1;
    }

    float getX2() {
        return mX2;
    }

    float getY2() {
        return mY2;
    }

    LinearShaderWrapper(final Gradient graphicGradient, final Rect bounds, final Matrix transform) {
        super(graphicGradient, transform);
        mX1 = getAbsoluteXCoordinate(bounds, graphicGradient.getX1());
        mY1 = getAbsoluteYCoordinate(bounds, graphicGradient.getY1());
        mX2 = getAbsoluteXCoordinate(bounds, graphicGradient.getX2());
        mY2 = getAbsoluteYCoordinate(bounds, graphicGradient.getY2());
    }

    @Override
    Shader getShader() {
        Shader linearGradient = new LinearGradient(mX1, mY1, mX2, mY2, getColorRange(), getInputRange(), getTileMode());
        linearGradient.setLocalMatrix(getTransform());
        return linearGradient;
    }
}
