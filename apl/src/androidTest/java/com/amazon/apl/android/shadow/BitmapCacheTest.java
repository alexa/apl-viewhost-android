/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;

import android.graphics.Bitmap;

import com.amazon.apl.android.bitmap.LruBitmapCache;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BitmapCacheTest {

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

    @Ignore("org.mockito.exceptions.base.MockitoException:\n" +
            "Cannot mock/spy class android.graphics.Bitmap\n" +
            "Mockito cannot mock/spy because :\n" +
            "- final or anonymous class")
    @Test
    public void testGetBitmap() {
        mCache.putBitmap(mMockKey, mMockBitmap);
        assertEquals(mMockBitmap, mCache.getBitmap(mMockKey));
    }

    @Ignore("org.mockito.exceptions.base.MockitoException:\n" +
            "Cannot mock/spy class android.graphics.Bitmap\n" +
            "Mockito cannot mock/spy because :\n" +
            "- final or anonymous class")
    @Test
    public void testEviction_noBitmapsRecycled() {
        // make sure we don't recycle evicted bitmaps
        mCache.putBitmap(mMockKey, mMockBitmap);
        mCache.clear();
        verify(mMockBitmap, never()).recycle();
    }
}
