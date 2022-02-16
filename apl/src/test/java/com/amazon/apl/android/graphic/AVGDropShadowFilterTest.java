/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Config(shadows={ViewhostRobolectricTest.MyShadowCanvas.class})
public class AVGDropShadowFilterTest extends ViewhostRobolectricTest {

    @Mock
    private Bitmap mMockBitmap;

    @Before
    public void before() {
        // Bitmap.extractAlpha() should return a bitmap Config.ALPHA_8 (which is 1 byte per pixel),
        // but Robolectric's Canvas implementation can only work on bitmaps of 4 bytes per pixel.
        when(mMockBitmap.extractAlpha(any(Paint.class), any())).thenReturn(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
    }

    @Test
    public void testApply_negativeRadius(){
        AVGDropShadowFilter dropShadowFilter = new AVGDropShadowFilter(0, -1, 1, 1);

        dropShadowFilter.apply(mMockBitmap, 1, 1);

        verifyZeroInteractions(mMockBitmap);
    }

    @Test
    public void testApply_radiusEqualsZero(){
        AVGDropShadowFilter dropShadowFilter = new AVGDropShadowFilter(0, 0, 1, 1);

        dropShadowFilter.apply(mMockBitmap, 1, 1);

        verify(mMockBitmap).extractAlpha(any(Paint.class), any());
    }

    @Test
    public void testApply_radiusEqualsZeroAfterScaling(){
        AVGDropShadowFilter dropShadowFilter = new AVGDropShadowFilter(0, 1, 1, 1);

        dropShadowFilter.apply(mMockBitmap, 0, 0);

        verify(mMockBitmap).extractAlpha(any(Paint.class), any());
    }

    @Test
    public void testApply_positiveRadius(){
        AVGDropShadowFilter dropShadowFilter = new AVGDropShadowFilter(0, 1, 1, 1);

        dropShadowFilter.apply(mMockBitmap, 1, 1);

        verify(mMockBitmap).extractAlpha(any(Paint.class), any());
    }

}
