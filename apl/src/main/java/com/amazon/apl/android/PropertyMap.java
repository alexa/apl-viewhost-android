/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.GraphicFilters;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.primitive.Radii;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.enums.APLEnum;
import com.amazon.apl.enums.ObjectType;

import java.util.Objects;

public abstract class PropertyMap<B extends BoundObject, K extends APLEnum> {

    @NonNull
    public abstract B getMapOwner();

    /**
     * Override this method with an implementation in order to access scaled properties.
     *
     * @return the metrics transform for scaling.
     */
    @Nullable
    protected abstract IMetricsTransform getMetricsTransform();

    // Returned from property getters when the native core object does not have a property by that key.
    public static final int NO_VALUE = -1;

    /**
     * Converts an `apl::Object` into a Java Object. Because an apl::Object can be an array
     * of `apl::Object`s, all primitives are converted to the Java class primitive wrappers. For
     * example, `bool` is converted to a `Boolean` not a `boolean`. This allows us to create
     * Object[] and Map<String, Object>. In general, though, use primitive types whenever possible.
     *
     * @param property Property to retrieve.
     * @param <T>      Return type.
     * @return Java Object representing this property
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public final <T> T get(K property) {
        return (T) nGet(getNativeHandle(), property.getIndex());
    }

    @NonNull
    public final ObjectType getType(K property) {
        return ObjectType.valueOf(nGetType(getNativeHandle(), property.getIndex()));
    }

    @NonNull
    public final Matrix getScaledTransform(K property) {
        final IMetricsTransform transform = Objects.requireNonNull(getMetricsTransform());
        Matrix m = new Matrix();
        float[] toMatrix = toMatrix(nGetTransform(getNativeHandle(), property.getIndex()));
        // Tx and Ty are scaled.
        toMatrix[2] = transform.toViewhost(toMatrix[2]);
        toMatrix[5] = transform.toViewhost(toMatrix[5]);
        m.setValues(toMatrix);
        return m;
    }

    @NonNull
    public final Matrix getTransform(K property) {
        Matrix m = new Matrix();
        float[] toMatrix = toMatrix(nGetTransform(getNativeHandle(), property.getIndex()));
        m.setValues(toMatrix);
        return m;
    }

    /**
     * Converts a core transform2D array into a float[9] in {@link Matrix} order.
     *
     * @param transform2D a float array representing a matrix in the order:
     *                    arr[0]  arr[2]  arr[4]
     *                    arr[1]  arr[3]  arr[5]
     * @return the float array in Matrix order.
     */
    private static float[] toMatrix(float[] transform2D) {
        return new float[] {
                transform2D[0], transform2D[2], transform2D[4],
                transform2D[1], transform2D[3], transform2D[5],
                0,              0,              1
        };
    }

    public final int getInt(K property) {
        return nGetInt(getNativeHandle(), property.getIndex());
    }

    public final int[] getIntArray(K property) {
        return nGetIntArray(getNativeHandle(), property.getIndex());
    }

    public final float getFloat(K property) {
        return nGetFloat(getNativeHandle(), property.getIndex());
    }

    public final float[] getFloatArray(K property) {
        return nGetFloatArray(getNativeHandle(), property.getIndex());
    }

    public final boolean getBoolean(K property) {
        return nGetBoolean(getNativeHandle(), property.getIndex());
    }

    public final String getString(K property) {
        int index = property.getIndex();
        if (nHasProperty(getNativeHandle(), index)) {
            return nGetString(getNativeHandle(), property.getIndex());
        }
        return null;
    }

    public final String[] getStringArray(K property) {
        int index = property.getIndex();
        if (nHasProperty(getNativeHandle(), index)) {
            return nGetStringArray(getNativeHandle(), index);
        }
        return null;
    }

    public final int getColor(K property) {
        long value = nGetColor(getNativeHandle(), property.getIndex());
        return ColorUtils.toARGB(value);
    }

    public final int getEnum(K property) {
        int index = property.getIndex();
        if (nHasProperty(getNativeHandle(), index)) {
            return nGetEnum(getNativeHandle(), index);
        }
        return NO_VALUE;
    }

    public final Dimension getDimension(K property) {
        final IMetricsTransform transform = Objects.requireNonNull(getMetricsTransform());
        if (hasProperty(property))
            return Dimension.create(getMapOwner(), property, transform);
        return null;
    }

    public final Radii getRadii(K property) {
        final IMetricsTransform transform = Objects.requireNonNull(getMetricsTransform());
        if (hasProperty(property))
            return Radii.create(getMapOwner(), property, transform);
        return null;
    }

    public final StyledText getStyledText(K property) {
        if (hasProperty(property))
            return new StyledText(getMapOwner(), property, getMetricsTransform());
        return null;
    }

    public final Gradient getGradient(K property) {
        if (hasProperty(property))
            return Gradient.create(getMapOwner(), property);
        return null;
    }

    public final MediaSources getMediaSources(K property) {
        if (hasProperty(property)) {
            return MediaSources.create(getMapOwner(), property);
        }
        return null;
    }

    public final Filters getFilters(K property) {
        final IMetricsTransform transform = Objects.requireNonNull(getMetricsTransform());
        if (hasProperty(property))
            return Filters.create(getMapOwner(), property, transform);
        return Filters.create();
    }

    public final Rect getRect(K property) {
        final IMetricsTransform transform = Objects.requireNonNull(getMetricsTransform());
        if (hasProperty(property)) {
            return Rect.create(getMapOwner(), property, transform);
        }
        return null;
    }

    public final AccessibilityActions getAccessibilityActions(K property) {
        if (hasProperty(property))
            return AccessibilityActions.create(getMapOwner(), property);
        return null;
    }

    public final GraphicFilters getGraphicFilters(K property) {
        if (hasProperty(property))
            return GraphicFilters.create(getMapOwner(), property);
        return null;
    }

    /**
     * @param property The property to verify.
     * @return True if the component has the property;
     */
    public final boolean hasProperty(K property) {
        return nHasProperty(getNativeHandle(), property.getIndex());
    }

    @NonNull
    private static native Object nGet(long nativeHandle, int propertyKey);
    private static native int nGetType(long nativeHandle, int propertyKey);
    @NonNull
    private static native float[] nGetTransform(long nativeHandle, int propertyKey);
    static native int nGetEnum(long nativeHandle, int propertyId);
    private static native boolean nHasProperty(long nativeHandle, int propertyKey);
    private static native int nGetInt(long nativeHandle, int propertyKey);
    private static native int[] nGetIntArray(long nativeHandle, int propertyKey);
    private static native float nGetFloat(long nativeHandle, int propertyKey);
    private static native float[] nGetFloatArray(long nativeHandle, int propertyKey);
    private static native boolean nGetBoolean(long nativeHandle, int propertyKey);
    @NonNull
    private static native String nGetString(long nativeHandle, int propertyKey);
    private static native String[] nGetStringArray(long nativeHandle, int propertyKey);
    private static native long nGetColor(long nativeHandle, int propertyKey);
    private long getNativeHandle() {
        return getMapOwner().getNativeHandle();
    }
}
