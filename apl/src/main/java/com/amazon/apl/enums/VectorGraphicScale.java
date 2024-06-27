/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum VectorGraphicScale implements APLEnum {

    kVectorGraphicScaleNone(0),
    kVectorGraphicScaleFill(1),
    kVectorGraphicScaleBestFill(2),
    kVectorGraphicScaleBestFit(3);

    private static SparseArray<VectorGraphicScale> values = null;

    static {
        VectorGraphicScale.values = new SparseArray<>();
        VectorGraphicScale[] values = VectorGraphicScale.values();
        for (VectorGraphicScale value : values) {
            VectorGraphicScale.values.put(value.getIndex(), value);
        }
    }

    public static VectorGraphicScale valueOf(int idx) {
        return VectorGraphicScale.values.get(idx);
    }

    private final int index;

    VectorGraphicScale (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}