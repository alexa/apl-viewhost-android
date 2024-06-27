/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum SpeechMarkType implements APLEnum {

    kSpeechMarkWord(0),
    kSpeechMarkSentence(1),
    kSpeechMarkSSML(2),
    kSpeechMarkViseme(3),
    kSpeechMarkUnknown(4);

    private static SparseArray<SpeechMarkType> values = null;

    static {
        SpeechMarkType.values = new SparseArray<>();
        SpeechMarkType[] values = SpeechMarkType.values();
        for (SpeechMarkType value : values) {
            SpeechMarkType.values.put(value.getIndex(), value);
        }
    }

    public static SpeechMarkType valueOf(int idx) {
        return SpeechMarkType.values.get(idx);
    }

    private final int index;

    SpeechMarkType (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
