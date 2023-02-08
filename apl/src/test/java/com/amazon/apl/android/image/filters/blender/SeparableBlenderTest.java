/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.blender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.ColorFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.GradientFilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.BlendMode;
import com.amazon.apl.enums.GradientType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

// This test class is intended to prevent regressions during refactors of the SeparableBlender class.
public class SeparableBlenderTest extends ViewhostRobolectricTest {

    static final int SRC_COLOR = Color.argb(255, 51, 211, 189);
    static final int DST_COLOR = Color.argb(255, 76, 39, 200);

    @Test
    public void ScreenTest() {
        blendAssertions(BlendMode.kBlendModeScreen, -9381135);
    }

    @Test
    public void OverlayTest() {
        blendAssertions(BlendMode.kBlendModeOverlay, -14794269);
    }

    @Test
    public void LightenTest() {
        blendAssertions(BlendMode.kBlendModeLighten, -11742264);
    }

    @Test
    public void DarkenTest() {
        blendAssertions(BlendMode.kBlendModeDarken, -13424707);
    }

    @Test
    public void ColorDodgeTest() {
        blendAssertions(BlendMode.kBlendModeColorDodge, -10493185);
    }

    @Test
    public void ColorBurnTest() {
        blendAssertions(BlendMode.kBlendModeColorBurn, -16777035);
    }

    @Test
    public void HardLightTest() {
        blendAssertions(BlendMode.kBlendModeHardLight, -14764829);
    }

    @Test
    public void SoftLightTest() {
        blendAssertions(BlendMode.kBlendModeSoftLight, -13873452);
    }

    @Test
    public void ExclusionTest() {
        blendAssertions(BlendMode.kBlendModeExclusion, -10372771);
    }

    @Test
    public void DifferenceTest() {
        blendAssertions(BlendMode.kBlendModeDifference, -15094773);
    }

    private void blendAssertions(BlendMode blendMode, int blendExpected) {
        SeparableBlender blender = new SeparableBlender(blendMode);

        assertEquals(blendExpected, blender.blendPixels(SRC_COLOR, DST_COLOR));
        assertEquals(SRC_COLOR, blender.blendPixels(SRC_COLOR, Color.TRANSPARENT));
        assertEquals(DST_COLOR, blender.blendPixels(Color.TRANSPARENT, DST_COLOR));
        assertEquals(Color.TRANSPARENT, blender.blendPixels(Color.TRANSPARENT, Color.TRANSPARENT));
    }
}
