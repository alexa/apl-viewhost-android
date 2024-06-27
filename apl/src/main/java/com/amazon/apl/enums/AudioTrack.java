/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum AudioTrack implements APLEnum {

    kAudioTrackBackground(0),
    kAudioTrackForeground(1),
    kAudioTrackNone(2);

    private static SparseArray<AudioTrack> values = null;

    static {
        AudioTrack.values = new SparseArray<>();
        AudioTrack[] values = AudioTrack.values();
        for (AudioTrack value : values) {
            AudioTrack.values.put(value.getIndex(), value);
        }
    }

    public static AudioTrack valueOf(int idx) {
        return AudioTrack.values.get(idx);
    }

    private final int index;

    AudioTrack (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}