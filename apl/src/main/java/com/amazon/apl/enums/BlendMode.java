/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum BlendMode implements APLEnum {

    kBlendModeNormal(0),
    kBlendModeMultiply(1),
    kBlendModeScreen(2),
    kBlendModeOverlay(3),
    kBlendModeDarken(4),
    kBlendModeLighten(5),
    kBlendModeColorDodge(6),
    kBlendModeColorBurn(7),
    kBlendModeHardLight(8),
    kBlendModeSoftLight(9),
    kBlendModeDifference(10),
    kBlendModeExclusion(11),
    kBlendModeHue(12),
    kBlendModeSaturation(13),
    kBlendModeColor(14),
    kBlendModeLuminosity(15),
    kBlendModeSourceAtop(16),
    kBlendModeSourceIn(17),
    kBlendModeSourceOut(18);

    private static SparseArray<BlendMode> values = null;

    static {
        BlendMode.values = new SparseArray<>();
        BlendMode[] values = BlendMode.values();
        for (BlendMode value : values) {
            BlendMode.values.put(value.getIndex(), value);
        }
    }

    public static BlendMode valueOf(int idx) {
        return BlendMode.values.get(idx);
    }

    private final int index;

    BlendMode (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
