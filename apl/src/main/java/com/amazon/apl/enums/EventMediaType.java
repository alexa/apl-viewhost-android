/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum EventMediaType implements APLEnum {

    kEventMediaTypeImage(0),
    kEventMediaTypeVideo(1),
    kEventMediaTypeVectorGraphic(2);

    private static SparseArray<EventMediaType> values = null;

    static {
        EventMediaType.values = new SparseArray<>();
        EventMediaType[] values = EventMediaType.values();
        for (EventMediaType value : values) {
            EventMediaType.values.put(value.getIndex(), value);
        }
    }

    public static EventMediaType valueOf(int idx) {
        return EventMediaType.values.get(idx);
    }

    private final int index;

    EventMediaType (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}