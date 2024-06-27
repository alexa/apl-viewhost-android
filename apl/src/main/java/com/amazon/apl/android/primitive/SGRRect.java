/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.google.auto.value.AutoValue;


/**
 * A simple SGRRectangle class.
 */
@AutoValue
public abstract class SGRRect {
    public abstract float getLeft();
    public abstract float getTop();
    public abstract float getRight();
    public abstract float getBottom();
    public abstract float[] getRadii();

    public int intLeft() {
        return Math.round(getLeft());
    }

    public int intTop() {
        return Math.round(getTop());
    }

    public int intRight() {
        return Math.round(getRight());
    }

    public int intBottom() {
        return Math.round(getBottom());
    }

    public static SGRRect create(final float[] bounds, final float[] radii) {
        return builder()
                .left(bounds[0])
                .top(bounds[1])
                .right(bounds[2])
                .bottom(bounds[3])
                .radii(radii)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SGRRect.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder left(float l);
        public abstract Builder top(float t);
        public abstract Builder right(float l);
        public abstract Builder bottom(float t);
        public abstract Builder radii(float[] r);
        public abstract SGRRect build();
    }
}