/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum Display implements APLEnum {

    kDisplayNormal(0),
    kDisplayInvisible(1),
    kDisplayNone(2);

    private static SparseArray<Display> values = null;

    static {
        Display.values = new SparseArray<>();
        Display[] values = Display.values();
        for (Display value : values) {
            Display.values.put(value.getIndex(), value);
        }
    }

    public static Display valueOf(int idx) {
        return Display.values.get(idx);
    }

    private final int index;

    Display (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
