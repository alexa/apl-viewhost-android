/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum DisplayState implements APLEnum {

    // Not visible
    kDisplayStateHidden(0),
    // Neither hidden nor foreground
    kDisplayStateBackground(1),
    // Visible and at the front
    kDisplayStateForeground(2);

    private static SparseArray<DisplayState> values = null;

    static {
        DisplayState.values = new SparseArray<>();
        DisplayState[] values = DisplayState.values();
        for (DisplayState value : values) {
            DisplayState.values.put(value.getIndex(), value);
        }
    }

    public static DisplayState valueOf(int idx) {
        return DisplayState.values.get(idx);
    }

    private final int index;

    DisplayState (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
