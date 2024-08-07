/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum GraphicTextAnchor implements APLEnum {

    kGraphicTextAnchorEnd(0),
    kGraphicTextAnchorMiddle(1),
    kGraphicTextAnchorStart(2);

    private static SparseArray<GraphicTextAnchor> values = null;

    static {
        GraphicTextAnchor.values = new SparseArray<>();
        GraphicTextAnchor[] values = GraphicTextAnchor.values();
        for (GraphicTextAnchor value : values) {
            GraphicTextAnchor.values.put(value.getIndex(), value);
        }
    }

    public static GraphicTextAnchor valueOf(int idx) {
        return GraphicTextAnchor.values.get(idx);
    }

    private final int index;

    GraphicTextAnchor (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
