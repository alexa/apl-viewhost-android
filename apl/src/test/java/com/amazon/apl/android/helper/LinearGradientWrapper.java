/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.helper;

import android.graphics.LinearGradient;
import android.graphics.Shader;

import java.util.Objects;

public class LinearGradientWrapper extends ShaderWrapper {

    public LinearGradientWrapper(LinearGradient linearGradient) {
        super(linearGradient);
    }

    public int getType() {
        return getWithReflection("mType");
    }

    public float getX0() {
        return getWithReflection("mX0");
    }

    public float getX1() {
        return getWithReflection("mX1");
    }

    public float getY0() {
        return getWithReflection("mY0");
    }

    public float getY1() {
        return getWithReflection("mY1");
    }

    public int[] getColors() {
        return getWithReflection("mColors");
    }

    public float[] getPositions() {
        return getWithReflection("mPositions");
    }

    public Shader.TileMode getTileMode() {
        return getWithReflection("mTileMode");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinearGradientWrapper that = (LinearGradientWrapper) o;
        return Objects.equals(getType(), that.getType())
                && Objects.equals(getX0(), that.getX0())
                && Objects.equals(getX1(), that.getX1())
                && Objects.equals(getY0(), that.getY0())
                && Objects.equals(getY1(), that.getY1())
                && Objects.deepEquals(getColors(), that.getColors())
                && Objects.deepEquals(getPositions(), that.getPositions())
                && Objects.equals(getTileMode(), that.getTileMode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getX0(), getX1(), getY0(), getY1(),
                getColors(), getPositions(), getTileMode());
    }
}
