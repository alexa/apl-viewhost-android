/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum TextTrackType implements APLEnum {

    kTextTrackTypeCaption(0);

    private static SparseArray<TextTrackType> values = null;

    static {
        TextTrackType.values = new SparseArray<>();
        TextTrackType[] values = TextTrackType.values();
        for (TextTrackType value : values) {
            TextTrackType.values.put(value.getIndex(), value);
        }
    }

    public static TextTrackType valueOf(int idx) {
        return TextTrackType.values.get(idx);
    }

    private final int index;

    TextTrackType (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}