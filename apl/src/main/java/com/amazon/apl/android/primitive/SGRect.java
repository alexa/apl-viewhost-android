/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.google.auto.value.AutoValue;


/**
 * A simple SGRectangle class.
 */
@AutoValue
public abstract class SGRect {
    public abstract float getLeft();
    public abstract float getTop();
    public abstract float getWidth();
    public abstract float getHeight();

    public int intLeft() {
        return Math.round(getLeft());
    }

    public int intTop() {
        return Math.round(getTop());
    }

    public int intRight() {
        return Math.round(getRight());
    }

    public float getRight() {
        return getLeft() + getWidth();
    }

    public int intBottom() {
        return Math.round(getBottom());
    }

    public float getBottom() {
        return getTop() + getHeight();
    }

    public int intHeight() {
        return Math.round(getHeight());
    }

    public int intWidth() {
        return Math.round(getWidth());
    }

    public static SGRect create(final float[] bounds) {
        return builder()
                .left(bounds[0])
                .top(bounds[1])
                .width(bounds[2])
                .height(bounds[3])
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SGRect.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder left(float l);
        public abstract Builder top(float t);
        public abstract Builder width(float w);
        public abstract Builder height(float h);
        public abstract SGRect build();
    }
}