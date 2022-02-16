/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Config(shadows={ViewhostRobolectricTest.MyShadowBitmap.class, ViewhostRobolectricTest.MyShadowCanvas.class})
public class PooledBitmapFactoryTest extends ViewhostRobolectricTest {

    PooledBitmapFactory pooledBitmapFactory;
    GlideCachingBitmapPool bitmapPool;
    @Mock
    ITelemetryProvider mockTelemetryProvider;

    @Before
    public void setup() {
        when(mockTelemetryProvider
                .createMetricId(anyString(), anyString(), any(ITelemetryProvider.Type.class)))
                .thenReturn(42);

        bitmapPool = new GlideCachingBitmapPool(new LruBitmapPool(10 * 1024 * 1024));
        pooledBitmapFactory = new PooledBitmapFactory(mockTelemetryProvider, bitmapPool);
    }

    @Test
    public void testPooledBitmapFactory_createBitmap() throws BitmapCreationException {
        Bitmap bitmap = pooledBitmapFactory.createBitmap(10, 20);
        assertEquals(10, bitmap.getWidth());
        assertEquals(20, bitmap.getHeight());

        bitmap.setPixel(0,0, Color.BLUE);
        Bitmap copy = pooledBitmapFactory.createBitmap(bitmap);
        assertNotEquals(copy, bitmap);
        assertEquals(copy.getHeight(), bitmap.getHeight());
        assertEquals(copy.getWidth(), bitmap.getWidth());
        assertEquals(bitmap.getPixel(0,0), copy.getPixel(0,0));
    }

    @Test
    public void testPooledBitmapFactory_createScaledBitmap() throws BitmapCreationException {
        Bitmap bitmap = pooledBitmapFactory.createBitmap(10, 20);
        Bitmap scaled = pooledBitmapFactory.createScaledBitmap(bitmap, 7, 14, true);
        assertNotEquals(bitmap, scaled);
        assertEquals(7, scaled.getWidth());
        assertEquals(14, scaled.getHeight());
    }

    @Test
    public void testPooledBitmapFactory_copy() throws BitmapCreationException {
        Bitmap bitmap = pooledBitmapFactory.createBitmap(10, 20);
        bitmap.setPixel(0,0, Color.BLUE);
        Bitmap copy = pooledBitmapFactory.copy(bitmap, true);
        assertNotEquals(bitmap, copy);
        assertEquals(bitmap.getPixel(0,0), copy.getPixel(0,0));
    }

    @Test
    public void testPooledBitmapFactory_disposeBitmap_returnsToPool()
            throws BitmapCreationException {
        Bitmap first = pooledBitmapFactory.createBitmap(10, 20);
        pooledBitmapFactory.disposeBitmap(first);
        Bitmap second = pooledBitmapFactory.createBitmap(10, 20);
        assertEquals(first, second);
    }

}