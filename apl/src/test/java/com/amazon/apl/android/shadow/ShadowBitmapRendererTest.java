/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.bitmap.PooledBitmapFactory;
import com.amazon.apl.android.bitmap.ShadowCache;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ShadowBitmapRendererTest extends ViewhostRobolectricTest {

    ShadowBitmapRenderer mRenderer;
    ShadowCache mCache;
    PooledBitmapFactory mMockBitmapFactory;

    @Before
    public void setUp() {
        mCache = new ShadowCache();
        mMockBitmapFactory = spy(PooledBitmapFactory.create(mock(ITelemetryProvider.class), mock(IBitmapPool.class)));
        mRenderer = new ShadowBitmapRenderer(mCache, mMockBitmapFactory);
    }

    // verify we do nothing when no shadow is set on the component
    @Test
    public void testPrepareShadow_NoShadow() {
        Component c = mock(Component.class);
        when(c.getShadowOffsetHorizontal()).thenReturn(0);
        when(c.getShadowOffsetVertical()).thenReturn(0);
        when(c.getShadowRadius()).thenReturn(0);
        when(c.getShadowRect()).thenReturn(mock(RectF.class));
        when(c.getShadowCornerRadius()).thenReturn(new float[]{0, 0, 0, 0});
        mRenderer.prepareShadow(c);
        ShadowBitmapKey key = new ShadowBitmapKey(c);
        assertNull(mCache.getShadow(key));
    }

    // Get that we get bitmap from cache when possible
    @Test
    public void testPrepareShadow_CachedUsed() {
        Component c = mock(Component.class);
        when(c.getShadowOffsetHorizontal()).thenReturn(1);
        when(c.getShadowOffsetVertical()).thenReturn(2);
        when(c.getShadowRadius()).thenReturn(3);
        when(c.getShadowRect()).thenReturn(new RectF(1, 1, 4, 4));
        when(c.getShadowCornerRadius()).thenReturn(new float[]{0, 1, 2, 3});
        // put bitmap for this component in cache
        ShadowBitmapKey key = new ShadowBitmapKey(c);
        Bitmap bitmap = Bitmap.createBitmap(10, 10,  Bitmap.Config.ARGB_8888);
        when(c.getShadowBitmap()).thenReturn(bitmap);
        mCache.putShadow(key, c);

        // verify renderer uses the cached bitmap now
        mRenderer.prepareShadow(c);
        verify(c).setShadowBitmap(bitmap);
        verifyNoInteractions(mMockBitmapFactory); // No new bitmap is created
    }

    @Test
    public void testPrepareShadow_caches_new() throws BitmapCreationException {
        Bitmap bitmap = Bitmap.createBitmap(10, 10,  Bitmap.Config.ARGB_8888);
        when(mMockBitmapFactory.createBitmap(3, 3)).thenReturn(bitmap);
        Component c = mock(Component.class);
        when(c.getShadowOffsetHorizontal()).thenReturn(1);
        when(c.getShadowOffsetVertical()).thenReturn(2);
        when(c.getShadowRadius()).thenReturn(0);
        when(c.getShadowRect()).thenReturn(new RectF(1, 1, 4, 4));
        when(c.getShadowCornerRadius()).thenReturn(new float[]{0, 1, 2, 3});
        mRenderer = new ShadowBitmapRenderer(mCache, mMockBitmapFactory);
        ShadowBitmapKey key = new ShadowBitmapKey(c);
        when(c.getShadowBitmap()).thenReturn(bitmap);
        mRenderer.prepareShadow(c);
        verify(c).setShadowBitmap(bitmap);
        assertEquals(bitmap, mCache.getShadow(key)); // The bitmap has been inserted into cache
    }
}
