/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import androidx.annotation.NonNull;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;


/**
 * A simple rectangle class.
 */
@AutoValue
public abstract class Rect {
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

    public static Rect create(BoundObject boundObject, APLEnum propertyKey, @NonNull IMetricsTransform transform) {
        float[] bounds = nGetRect(boundObject.getNativeHandle(), propertyKey.getIndex());
        return builder()
                .left(transform.toViewhost(bounds[0]))
                .top(transform.toViewhost(bounds[1]))
                .width(transform.toViewhost(bounds[2]))
                .height(transform.toViewhost(bounds[3]))
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Rect.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder left(float l);
        public abstract Builder top(float t);
        public abstract Builder width(float w);
        public abstract Builder height(float h);
        public abstract Rect build();
    }

    private static native float[] nGetRect(long componentHandle, int componentPropertyKey);

}