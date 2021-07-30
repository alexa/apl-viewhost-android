/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import androidx.annotation.NonNull;

import com.amazon.apl.android.BoundObject;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;


/**
 * Border Radii class representing the four corners of a border.
 */
@AutoValue
public abstract class Radii {
    /**
     * @return The top left border radius.
     */
    public abstract float topLeft();
    /**
     * @return The top right border radius.
     */
    public abstract float topRight();
    /**
     * @return The bottom right border radius.
     */
    public abstract float bottomRight();
    /**
     * @return The bottom left border radius.
     */
    public abstract float bottomLeft();

    static Builder builder() {
        return new AutoValue_Radii.Builder();
    }

    public static Radii create(BoundObject boundObject, APLEnum propertyKey, @NonNull IMetricsTransform transform) {
        return Radii.builder()
                .topLeft(transform.toViewhost(nGetTopLeft(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .topRight(transform.toViewhost(nGetTopRight(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .bottomRight(transform.toViewhost(nGetBottomRight(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .bottomLeft(transform.toViewhost(nGetBottomLeft(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .build();
    }

    /**
     * Convenience method to convert the radii to an 8 float array in the order:
     *      topLeft, topRight, bottomRight, bottomLeft
     * @return this Radii as an array of 8 floats.
     */
    public float[] toFloatArray() {
        return new float[] {
                topLeft(), topLeft(),
                topRight(), topRight(),
                bottomRight(), bottomRight(),
                bottomLeft(), bottomLeft()
        };
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder topLeft(float tl);
        abstract Builder topRight(float tr);
        abstract Builder bottomRight(float br);
        abstract Builder bottomLeft(float bl);
        abstract Radii build();
    }

    private static native float nGetTopLeft(long componentHandle, int componentPropertyKey);
    private static native float nGetTopRight(long componentHandle, int componentPropertyKey);
    private static native float nGetBottomRight(long componentHandle, int componentPropertyKey);
    private static native float nGetBottomLeft(long componentHandle, int componentPropertyKey);
}
