/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.bitmap.LruBitmapCache;
import com.amazon.apl.android.bitmap.PooledBitmapFactory;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShadowBitmapRendererTest extends ViewhostRobolectricTest {

    ShadowBitmapRenderer mRenderer;
    LruBitmapCache mMockCache;

    @Before
    public void setUp() {
        mMockCache = mock(LruBitmapCache.class);
        mRenderer = new ShadowBitmapRenderer(mMockCache, PooledBitmapFactory.create(mock(ITelemetryProvider.class), mock(IBitmapPool.class)));
    }

    // verify we do nothing when no shadow is set on the component
    @Test
    public void testPrepareShadow_NoShadow() {
        Component c = mock(Component.class);
        when(c.getShadowOffsetHorizontal()).thenReturn(0);
        when(c.getShadowOffsetVertical()).thenReturn(0);
        when(c.getShadowRadius()).thenReturn(0);
        mRenderer.prepareShadow(c);
        Mockito.verifyNoInteractions(mMockCache);
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
        when(mMockCache.getBitmap(key)).thenReturn(bitmap);

        // verify renderer uses the cached bitmap now
        mRenderer.prepareShadow(c);
        verify(mMockCache, times(1)).getBitmap(key); // once to check if in cache and again to get it
    }
}
