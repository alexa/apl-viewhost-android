/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum ImageAlign implements APLEnum {

    kImageAlignBottom(0),
    kImageAlignBottomLeft(1),
    kImageAlignBottomRight(2),
    kImageAlignCenter(3),
    kImageAlignLeft(4),
    kImageAlignRight(5),
    kImageAlignTop(6),
    kImageAlignTopLeft(7),
    kImageAlignTopRight(8);

    private static SparseArray<ImageAlign> values = null;

    static {
        ImageAlign.values = new SparseArray<>();
        ImageAlign[] values = ImageAlign.values();
        for (ImageAlign value : values) {
            ImageAlign.values.put(value.getIndex(), value);
        }
    }

    public static ImageAlign valueOf(int idx) {
        return ImageAlign.values.get(idx);
    }

    private final int index;

    ImageAlign (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}