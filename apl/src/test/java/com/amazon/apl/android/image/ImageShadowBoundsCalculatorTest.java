/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.graphics.RectF;

import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.ImageAlign;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ImageShadowBoundsCalculatorTest extends ViewhostRobolectricTest {

    @Mock
    private Rect mockBounds;
    @Mock
    private Rect mockInnerBounds;

    @Before
    public void setup() {
        when(mockBounds.getLeft()).thenReturn(0f);
        when(mockBounds.getTop()).thenReturn(0f);

        when(mockInnerBounds.intWidth()).thenReturn(198);
        when(mockInnerBounds.intHeight()).thenReturn(98);
        when(mockInnerBounds.getWidth()).thenReturn(198f);
        when(mockInnerBounds.getHeight()).thenReturn(98f);
    }

    @Test
    public void test_shadowBounds_kImageAlignBottom() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignBottom)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(21f, 32f, 217f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignTop() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignTop)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(21f, 32f, 217f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignBottomLeft() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignBottomLeft)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(20f, 32f, 216f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignBottomRight() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignBottomRight)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(22f, 32f, 218f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignLeft() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignLeft)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(20f, 32f, 216f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignRight() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignRight)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(22f, 32f, 218f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignTopLeft() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignTopLeft)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(20f, 32f, 216f, 130f, shadowBounds);
    }

    @Test
    public void test_shadowBounds_kImageAlignTopRight() {
        RectF shadowBounds = ImageShadowBoundsCalculator.builder()
                .align(ImageAlign.kImageAlignTopRight)
                .bounds(mockBounds)
                .innerBounds(mockInnerBounds)
                .imageBounds(getImageBounds())
                .offsetX(20)
                .offsetY(32)
                .build()
                .calculateShadowBounds();
        verifyShadowBounds(22f, 32f, 218f, 130f, shadowBounds);
    }

    private android.graphics.Rect getImageBounds() {
        return new android.graphics.Rect(0, 0, 196, 98);
    }

    private void verifyShadowBounds(float left, float top, float right, float bottom, RectF shadowBounds) {
        assertEquals(left, shadowBounds.left, 0.01f);
        assertEquals(top, shadowBounds.top, 0.01f);
        assertEquals(right, shadowBounds.right, 0.01f);
        assertEquals(bottom, shadowBounds.bottom, 0.01f);
    }
}
