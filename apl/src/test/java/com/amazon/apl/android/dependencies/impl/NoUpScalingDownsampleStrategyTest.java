/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NoUpScalingDownsampleStrategyTest extends ViewhostRobolectricTest {

    private final DownsampleStrategy strategy = new GlideImageLoader.NoUpscalingDownsampleStrategy();

    @Test
    public void test_noUpscaling() {
        float scaleFactor = strategy.getScaleFactor(100, 100, 200, 200);
        assertEquals(1.0f, scaleFactor, 0.01f);
    }

    @Test
    public void test_coversHeight() {
        float scaleFactor = strategy.getScaleFactor(100, 100, 50, 100);
        assertEquals(1.0f, scaleFactor, 0.01f);
    }

    @Test
    public void test_coversWidth() {
        float scaleFactor = strategy.getScaleFactor(100, 100, 100, 50);
        assertEquals(1.0f, scaleFactor, 0.01f);
    }

    @Test
    public void test_downScales() {
        float scaleFactor = strategy.getScaleFactor(200, 200, 100, 100);
        assertEquals(0.5f, scaleFactor, 0.01f);
    }

    @Test
    public void test_downScales_coversWidth() {
        float scaleFactor = strategy.getScaleFactor(200, 200, 100, 50);
        assertEquals(0.5f, scaleFactor, 0.01f);
    }

    @Test
    public void test_downScales_coversHeight() {
        float scaleFactor = strategy.getScaleFactor(200, 200, 50, 100);
        assertEquals(0.5f, scaleFactor, 0.01f);
    }

    @Test
    public void test_quality() {
        assertEquals(DownsampleStrategy.SampleSizeRounding.QUALITY, strategy.getSampleSizeRounding(100, 100, 200, 200));
    }
}
