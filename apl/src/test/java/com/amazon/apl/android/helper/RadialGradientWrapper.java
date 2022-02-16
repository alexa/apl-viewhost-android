/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.helper;

import android.graphics.RadialGradient;
import android.graphics.Shader;

import androidx.annotation.Nullable;

import java.util.Objects;

public class RadialGradientWrapper extends ShaderWrapper {

    public RadialGradientWrapper(RadialGradient radialGradient) {
        super(radialGradient);
    }

    public int getType() {
        return getWithReflection("mType");
    }

    public float getX() {
        return getWithReflection("mX");
    }

    public float getY() {
        return getWithReflection("mY");
    }

    public float getRadius() {
        return getWithReflection("mRadius");
    }

    public int[] getColors() {
        return getWithReflection("mColors");
    }

    public float[] getPositions() {
        return getWithReflection("mPositions");
    }

    public int getCenterColor() {
        return getWithReflection("mCenterColor");
    }

    public int getEdgeColor() {
        return getWithReflection("mEdgeColor");
    }

    public Shader.TileMode getTileMode() {
        return getWithReflection("mTileMode");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadialGradientWrapper that = (RadialGradientWrapper) o;
        return Objects.equals(getType(), that.getType())
                && Objects.equals(getX(), that.getX())
                && Objects.equals(getY(), that.getY())
                && Objects.deepEquals(getColors(), that.getColors())
                && Objects.deepEquals(getPositions(), that.getPositions())
                && Objects.equals(getCenterColor(), that.getCenterColor())
                && Objects.equals(getEdgeColor(), that.getEdgeColor())
                && Objects.equals(getTileMode(), that.getTileMode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getX(), getY(), getColors(), getPositions(),
                getCenterColor(), getEdgeColor(), getTileMode());
    }
}
