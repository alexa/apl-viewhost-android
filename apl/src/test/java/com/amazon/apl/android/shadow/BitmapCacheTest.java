/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;

import android.graphics.Bitmap;

import com.amazon.apl.android.bitmap.LruBitmapCache;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitmapCacheTest extends ViewhostRobolectricTest {

    private LruBitmapCache mCache;
    private Bitmap mMockBitmap;
    private ShadowBitmapKey mMockKey;

    @Before
    public void setUp() {
        mCache = new LruBitmapCache();
        mMockBitmap = mock(Bitmap.class);
        when(mMockBitmap.getByteCount()).thenReturn(100);
        mMockKey = mock(ShadowBitmapKey.class);
    }

    @Test
    public void testGetBitmap() {
        mCache.putBitmap(mMockKey, mMockBitmap);
        assertEquals(mMockBitmap, mCache.getBitmap(mMockKey));
    }

    @Test
    public void testEviction_noBitmapsRecycled() {
        // make sure we don't recycle evicted bitmaps
        mCache.putBitmap(mMockKey, mMockBitmap);
        mCache.clear();
        verify(mMockBitmap, never()).recycle();
    }
}
