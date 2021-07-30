/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.view.Gravity;

import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ImageScaleCalculatorTest {

    /**
     * Same set of tests for testImageScaleCalculator_BestFitCase but using different expected results
     */
    @Test
    public void testGetScale_NoneCase() {
        float[] scales;

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleNone, 1000, 1000, 1000, 400);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleNone, 1000, 1000, 400, 1000);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleNone, 1000, 1000, 500, 500);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleNone, 1000, 1000, 600, 400);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleNone, 1000, 1000, 2000, 1500);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);
    }

    /**
     * Same set of tests for testImageScaleCalculator_BestFitCase but using different expected results
     */
    @Test
    public void testGetScale_FillCase() {
        float[] scales;

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleFill, 1000, 1000, 1000, 400);
        assertArrayEquals(new float[]{1.0f, 2.5f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleFill, 1000, 1000, 400, 1000);
        assertArrayEquals(new float[]{2.5f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleFill, 1000, 1000, 500, 500);
        assertArrayEquals(new float[]{2.0f, 2.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleFill, 1000, 1000, 600, 400);
        assertArrayEquals(new float[]{1.66f, 2.5f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleFill, 1000, 1000, 2000, 1500);
        assertArrayEquals(new float[]{0.5f, 0.66f}, scales, 0.01f);
    }

    /**
     * Same set of tests for testImageScaleCalculator_BestFitCase but using different expected results
     */
    @Test
    public void testGetScale_BestFillCase() {
        float[] scales;

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFill, 1000, 1000, 1000, 400);
        assertArrayEquals(new float[]{2.5f, 2.5f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFill, 1000, 1000, 400, 1000);
        assertArrayEquals(new float[]{2.5f, 2.5f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFill, 1000, 1000, 500, 500);
        assertArrayEquals(new float[]{2.0f, 2.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFill, 1000, 1000, 600, 400);
        assertArrayEquals(new float[]{2.5f, 2.5f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFill, 1000, 1000, 2000, 1500);
        assertArrayEquals(new float[]{0.66f, 0.66f}, scales, 0.01f);
    }

    @Test
    public void testGetScale_BestFitCase() {
        float[] scales;

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFit, 1000, 1000, 1000, 400);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFit, 1000, 1000, 400, 1000);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFit, 1000, 1000, 500, 500);
        assertArrayEquals(new float[]{2.0f, 2.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFit, 1000, 1000, 600, 400);
        assertArrayEquals(new float[]{1.66f, 1.66f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFit, 1000, 1000, 2000, 1500);
        assertArrayEquals(new float[]{0.5f, 0.5f}, scales, 0.01f);
    }

    /**
     * Same set of tests for testImageScaleCalculator_BestFitCase but using different expected results
     */
    @Test
    public void testGetScale_BestFitDownCase() {
        float[] scales;

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFitDown, 1000, 1000, 1000, 400);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFitDown, 1000, 1000, 400, 1000);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFitDown, 1000, 1000, 500, 500);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFitDown, 1000, 1000, 600, 400);
        assertArrayEquals(new float[]{1.0f, 1.0f}, scales, 0.01f);

        scales = ImageScaleCalculator.getScale(ImageScale.kImageScaleBestFitDown, 1000, 1000, 2000, 1500);
        assertArrayEquals(new float[]{0.5f, 0.5f}, scales, 0.01f);
    }

    @Test
    public void testGetGravity() {
        assertEquals(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignBottom));
        assertEquals(Gravity.LEFT | Gravity.BOTTOM, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignBottomLeft));
        assertEquals(Gravity.RIGHT | Gravity.BOTTOM, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignBottomRight));
        assertEquals(Gravity.LEFT | Gravity.CENTER_VERTICAL, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignLeft));
        assertEquals(Gravity.RIGHT | Gravity.CENTER_VERTICAL, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignRight));
        assertEquals(Gravity.CENTER_HORIZONTAL | Gravity.TOP, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignTop));
        assertEquals(Gravity.LEFT | Gravity.TOP, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignTopLeft));
        assertEquals(Gravity.RIGHT | Gravity.TOP, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignTopRight));
        assertEquals(Gravity.CENTER, ImageScaleCalculator.getGravity(ImageAlign.kImageAlignCenter));
    }
}
