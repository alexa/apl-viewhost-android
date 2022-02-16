/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;

/**
 * Dimension.
 */
@AutoValue
public abstract class Dimension {
    /**
     * @return the dimension value as a float.
     */
    public abstract float value();

    public static Dimension create(BoundObject boundObject, APLEnum propertyKey, IMetricsTransform transform) {
        return new AutoValue_Dimension(transform.toViewhost((float)nGetValue(boundObject.getNativeHandle(), propertyKey.getIndex())));
    }

    public static Dimension create(float value) {
        return new AutoValue_Dimension(value);
    }

    /**
     * @return the dimension value as a int.
     */
    public int intValue() {
        return Math.round(value());
    }

    private static native double nGetValue(long ownerHandle, int propertyKey);
}