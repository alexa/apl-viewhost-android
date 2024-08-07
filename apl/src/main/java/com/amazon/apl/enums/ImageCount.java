/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum ImageCount implements APLEnum {

    ZERO(0),
    ONE(1),
    TWO(2);

    private static SparseArray<ImageCount> values = null;

    static {
        ImageCount.values = new SparseArray<>();
        ImageCount[] values = ImageCount.values();
        for (ImageCount value : values) {
            ImageCount.values.put(value.getIndex(), value);
        }
    }

    public static ImageCount valueOf(int idx) {
        return ImageCount.values.get(idx);
    }

    private final int index;

    ImageCount (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
