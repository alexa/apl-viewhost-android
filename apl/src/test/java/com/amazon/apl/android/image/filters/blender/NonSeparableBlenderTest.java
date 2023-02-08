/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.blender;

import static org.junit.Assert.assertEquals;

import android.graphics.Color;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.BlendMode;

import org.junit.Test;

// This test class is intended to prevent regressions during refactors of the NonSeparableBlender class.
public class NonSeparableBlenderTest extends ViewhostRobolectricTest {

    static final int SRC_COLOR = Color.argb(255, 51, 211, 189);
    static final int DST_COLOR = Color.argb(255, 76, 39, 200);

    @Test
    public void HueTest() {
        blendAssertions(BlendMode.kBlendModeHue, -16751787);
    }

    @Test
    public void ColorTest() {
        blendAssertions(BlendMode.kBlendModeColor, -16751787);
    }

    @Test
    public void SaturationTest() {
        blendAssertions(BlendMode.kBlendModeSaturation, -11786297);
    }

    @Test
    public void LuminosityTest() {
        blendAssertions(BlendMode.kBlendModeLuminosity, -5862145);
    }

    private void blendAssertions(BlendMode blendMode, int blendExpected) {
        NonSeparableBlender blender = new NonSeparableBlender(blendMode);

        assertEquals(blendExpected, blender.blendPixels(SRC_COLOR, DST_COLOR));
        assertEquals(SRC_COLOR, blender.blendPixels(SRC_COLOR, Color.TRANSPARENT));
        assertEquals(DST_COLOR, blender.blendPixels(Color.TRANSPARENT, DST_COLOR));
        assertEquals(Color.TRANSPARENT, blender.blendPixels(Color.TRANSPARENT, Color.TRANSPARENT));
    }
}
