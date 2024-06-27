/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.google.auto.value.AutoValue;


/**
 * Border SGRadii class representing the four corners of a border.
 */
@AutoValue
public abstract class SGRadii {
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
        return new AutoValue_SGRadii.Builder();
    }

    public static SGRadii create(float[] radii) {
        return SGRadii.builder()
                .topLeft(radii[0])
                .topRight(radii[1])
                .bottomRight(radii[2])
                .bottomLeft(radii[3])
                .build();
    }

    /**
     * Convenience method to convert the SGRadii to an 8 float array in the order:
     *      topLeft, topRight, bottomRight, bottomLeft
     * @return this SGRadii as an array of 8 floats.
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
        abstract SGRadii build();
    }
}
