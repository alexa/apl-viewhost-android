/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum KeyboardType implements APLEnum {

    kKeyboardTypeDecimalPad(0),
    kKeyboardTypeEmailAddress(1),
    kKeyboardTypeNormal(2),
    kKeyboardTypeNumberPad(3),
    kKeyboardTypePhonePad(4),
    kKeyboardTypeUrl(5);

    private static SparseArray<KeyboardType> values = null;

    static {
        KeyboardType.values = new SparseArray<>();
        KeyboardType[] values = KeyboardType.values();
        for (KeyboardType value : values) {
            KeyboardType.values.put(value.getIndex(), value);
        }
    }

    public static KeyboardType valueOf(int idx) {
        return KeyboardType.values.get(idx);
    }

    private final int index;

    KeyboardType (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
