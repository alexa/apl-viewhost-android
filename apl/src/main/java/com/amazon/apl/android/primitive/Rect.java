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
        return builder()
                .left(transform.toViewhost(nGetLeft(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .top(transform.toViewhost(nGetTop(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .width(transform.toViewhost(nGetWidth(boundObject.getNativeHandle(), propertyKey.getIndex())))
                .height(transform.toViewhost(nGetHeight(boundObject.getNativeHandle(), propertyKey.getIndex())))
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

    private static native float nGetLeft(long componentHandle, int componentPropertyKey);
    private static native float nGetTop(long componentHandle, int componentPropertyKey);
    private static native float nGetWidth(long componentHandle, int componentPropertyKey);
    private static native float nGetHeight(long componentHandle, int componentPropertyKey);
}