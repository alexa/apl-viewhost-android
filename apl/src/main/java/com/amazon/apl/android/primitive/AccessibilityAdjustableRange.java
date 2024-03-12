/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.common.BoundObject;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;

/**
 * AccessibilityAdjustableRange Property
 * Provide values as rangeInfo to accessibility Node to expose to screen reader
 */
@AutoValue
public abstract class AccessibilityAdjustableRange {
    public abstract float minValue();
    public abstract float maxValue();
    public abstract float currentValue();

    public static AccessibilityAdjustableRange create(BoundObject boundObject, APLEnum propertyKey) {
        return new AutoValue_AccessibilityAdjustableRange(
                nGetMinValue(boundObject.getNativeHandle(), propertyKey.getIndex()),
                nGetMaxValue(boundObject.getNativeHandle(), propertyKey.getIndex()),
                nGetCurrentValue(boundObject.getNativeHandle(), propertyKey.getIndex())
        );
    }

    public static AccessibilityAdjustableRange create(float minValue, float maxValue, float currentValue) {
        return new AutoValue_AccessibilityAdjustableRange(minValue, maxValue, currentValue);
    }

    private static native float nGetMinValue(long componentHandle, int componentPropertyKey);
    private static native float nGetMaxValue(long componentHandle, int componentPropertyKey);
    private static native float nGetCurrentValue(long componentHandle, int componentPropertyKey);
    private static native Object nGetAdjustableValue(long componentHandle, int componentPropertyKey);
}
