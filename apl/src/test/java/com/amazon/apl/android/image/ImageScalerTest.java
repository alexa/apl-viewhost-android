/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.BitmapFactory;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.ImageScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.amazon.apl.enums.ImageAlign.kImageAlignCenter;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ImageScalerTest extends ViewhostRobolectricTest {
    @Mock
    private Rect mockInnerBounds;
    @Mock
    private BitmapFactory mockBitmapFactory;

    @Before
    public void setup() {
        when(mockInnerBounds.intWidth()).thenReturn(198);
        when(mockInnerBounds.intHeight()).thenReturn(98);
        when(mockInnerBounds.getWidth()).thenReturn(198f);
        when(mockInnerBounds.getHeight()).thenReturn(98f);
    }

    @Test
    public void testGetScaledBitmap_ImageScaleNone() throws BitmapCreationException {
        when(mockBitmapFactory.createBitmap(
                any(Bitmap.class), eq(0), eq(0), eq(196), eq(98),
                any(Matrix.class), eq(true))).thenReturn(createDummyBitmap());
        Bitmap scaledBitmap = ImageScaler.getScaledBitmap(mockInnerBounds, ImageScale.kImageScaleNone, kImageAlignCenter, mockBitmapFactory, createDummyBitmap());
        assertEquals(196, scaledBitmap.getWidth());
        assertEquals(98, scaledBitmap.getHeight());
    }

    @Test
    public void testGetScaledBitmap_ImageScaleFill() throws BitmapCreationException {
        when(mockBitmapFactory.createBitmap(
                any(Bitmap.class), eq(0), eq(0), eq(196), eq(98),
                any(Matrix.class), eq(true))).thenReturn(createDummyBitmap());
        Bitmap scaledBitmap = ImageScaler.getScaledBitmap(mockInnerBounds, ImageScale.kImageScaleFill, kImageAlignCenter, mockBitmapFactory, createDummyBitmap());
        assertEquals(196, scaledBitmap.getWidth());
        assertEquals(98, scaledBitmap.getHeight());
    }

    @Test
    public void testGetScaledBitmap_ImageScaleBestFill() throws BitmapCreationException {
        when(mockBitmapFactory.createBitmap(
                any(Bitmap.class), eq(0), eq(1), eq(196), eq(97),
                any(Matrix.class), eq(true))).thenReturn(createDummyBitmap());
        Bitmap scaledBitmap = ImageScaler.getScaledBitmap(mockInnerBounds, ImageScale.kImageScaleBestFill, kImageAlignCenter, mockBitmapFactory, createDummyBitmap());
        assertEquals(196, scaledBitmap.getWidth());
        assertEquals(98, scaledBitmap.getHeight());
    }

    @Test
    public void testGetScaledBitmap_ImageScaleBestFit() throws BitmapCreationException {
        when(mockBitmapFactory.createBitmap(
                any(Bitmap.class), eq(0), eq(0), eq(196), eq(98),
                any(Matrix.class), eq(true))).thenReturn(createDummyBitmap());
        Bitmap scaledBitmap = ImageScaler.getScaledBitmap(mockInnerBounds, ImageScale.kImageScaleBestFit, kImageAlignCenter, mockBitmapFactory, createDummyBitmap());
        assertEquals(196, scaledBitmap.getWidth());
        assertEquals(98, scaledBitmap.getHeight());
    }

    @Test
    public void testGetScaledBitmap_ImageScaleBestFitDown() throws BitmapCreationException {
        when(mockBitmapFactory.createBitmap(
                any(Bitmap.class), eq(0), eq(0), eq(196), eq(98),
                any(Matrix.class), eq(true))).thenReturn(createDummyBitmap());
        Bitmap scaledBitmap = ImageScaler.getScaledBitmap(mockInnerBounds, ImageScale.kImageScaleBestFitDown, kImageAlignCenter, mockBitmapFactory, createDummyBitmap());
        assertEquals(196, scaledBitmap.getWidth());
        assertEquals(98, scaledBitmap.getHeight());
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(196, 98, Bitmap.Config.ARGB_8888);
    }
}
