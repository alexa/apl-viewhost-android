/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum GradientSpreadMethod implements APLEnum {

    PAD(0),
    REFLECT(1),
    REPEAT(2);

    private static SparseArray<GradientSpreadMethod> values = null;

    static {
        GradientSpreadMethod.values = new SparseArray<>();
        GradientSpreadMethod[] values = GradientSpreadMethod.values();
        for (GradientSpreadMethod value : values) {
            GradientSpreadMethod.values.put(value.getIndex(), value);
        }
    }

    public static GradientSpreadMethod valueOf(int idx) {
        return GradientSpreadMethod.values.get(idx);
    }

    private final int index;

    GradientSpreadMethod (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
