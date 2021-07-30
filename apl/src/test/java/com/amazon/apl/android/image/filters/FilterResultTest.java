/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

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
import com.amazon.apl.enums.GradientType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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

public class FilterResultTest extends ViewhostRobolectricTest {
    private FilterResult mFilterResult;
    @Mock
    private IBitmapFactory mBitmapFactory;

    @Before
    public void setup() {
        try {
            when(mBitmapFactory.createBitmap(anyInt(), anyInt())).thenReturn(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            when(mBitmapFactory.createBitmap(any(Bitmap.class), anyInt(), anyInt(), anyInt(), anyInt(), eq(null), anyBoolean()))
                    .thenReturn(Bitmap.createBitmap(5, 10, Bitmap.Config.ARGB_8888));
        } catch (BitmapCreationException e) {

        }
    }

    @Test
    public void testFilterResult_actualBitmap_returnsBitmap() {
        final int width = 10;
        final int height = 20;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        mFilterResult = new BitmapFilterResult(source, mBitmapFactory);
        assertTrue(mFilterResult.isBitmap());
        assertEquals(Size.create(width, height), mFilterResult.getSize());
        assertEquals(source, mFilterResult.getBitmap());
    }

    @Test
    public void testFilterResult_actualBitmap_smallerSize() {
        final int width = 10;
        final int height = 20;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        mFilterResult = new BitmapFilterResult(source, mBitmapFactory);
        Bitmap truncated = mFilterResult.getBitmap(Size.create(5, 10));
        assertEquals(5, truncated.getWidth());
        assertEquals(10, truncated.getHeight());
    }

    @Test
    public void testFilterResult_actualBitmap_greaterSize() throws BitmapCreationException {
        final int width = 10;
        final int height = 20;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        mFilterResult = new BitmapFilterResult(source, mBitmapFactory);
        mFilterResult.getBitmap(Size.create(15, 30));
        verify(mBitmapFactory).createBitmap(15, 30);
    }

    @Test
    public void testFilterResult_actualBitmap_unEqualSize() throws BitmapCreationException {
        final int width = 5;
        final int height = 10;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        mFilterResult = new BitmapFilterResult(source, mBitmapFactory);
        mFilterResult.getBitmap(Size.create(1, 20));
        verify(mBitmapFactory).createBitmap(1, 20);
    }

    @Test
    public void testFilterResult_colorBitmap() throws BitmapCreationException {
        mFilterResult = new ColorFilterResult(Color.BLUE, mBitmapFactory);

        assertFalse(mFilterResult.isBitmap());
        assertEquals(Size.ZERO, mFilterResult.getSize());
        try {
            mFilterResult.getBitmap();
            fail("Can't call getBitmap() on a zero size bitmap.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        mFilterResult.getBitmap(Size.create(10, 20));
        verify(mBitmapFactory).createBitmap(10, 20);
    }

    @Test
    public void testFilterResult_gradientBitmap() throws BitmapCreationException {
        mFilterResult = new GradientFilterResult(Gradient.builder()
                .type(GradientType.LINEAR)
                .inputRange(new float[] {0.25f, 0.75f})
                .colorRange(new int[]{Color.BLACK, Color.TRANSPARENT})
                .angle(90f)
                .build(), mBitmapFactory);

        assertFalse(mFilterResult.isBitmap());
        assertEquals(Size.ZERO, mFilterResult.getSize());
        try {
            mFilterResult.getBitmap();
            fail("Can't call getBitmap() on a zero size bitmap.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        mFilterResult.getBitmap(Size.create(10, 20));
        verify(mBitmapFactory).createBitmap(10, 20);
    }
}
