/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.content.ComponentCallbacks2;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class LruPackageCacheTest extends ViewhostRobolectricTest {

    private LruPackageCache mLruPackageCache;

    @Mock
    APLJSONData mPackageOne;
    @Mock
    APLJSONData mPackageTwo;

    @Before
    public void setup() {
        mLruPackageCache = new LruPackageCache(100);
    }

    @Test
    public void testPutAndGet() {
        when(mPackageOne.getSize()).thenReturn(25);
        mLruPackageCache.put(Content.ImportRef.create("my-package", "1.0"), mPackageOne);
        assertEquals(mPackageOne, mLruPackageCache.get(Content.ImportRef.create("my-package", "1.0")));
    }

    @Test
    public void testLaterEntriesEvictEarlierEntries() {
        when(mPackageOne.getSize()).thenReturn(25);
        when(mPackageTwo.getSize()).thenReturn(50);
        mLruPackageCache.put(Content.ImportRef.create("p", "1.0"), mPackageOne);
        mLruPackageCache.put(Content.ImportRef.create("p", "1.1"), mPackageTwo);

        // First is evicted
        assertNull(mLruPackageCache.get(Content.ImportRef.create("p", "1.0")));
        // Second remains
        assertEquals(mPackageTwo, mLruPackageCache.get(Content.ImportRef.create("p", "1.1")));
    }

    @Test
    public void testOnTrimMemoryCriticalEvictsLaterEntries() {
        when(mPackageOne.getSize()).thenReturn(25);
        when(mPackageTwo.getSize()).thenReturn(25);
        mLruPackageCache.put(Content.ImportRef.create("p", "1.0"), mPackageOne);
        mLruPackageCache.put(Content.ImportRef.create("p", "1.1"), mPackageTwo);

        mLruPackageCache.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);

        // First is evicted
        assertNull(mLruPackageCache.get(Content.ImportRef.create("p", "1.0")));
        // Second remains
        assertEquals(mPackageTwo, mLruPackageCache.get(Content.ImportRef.create("p", "1.1")));
    }

    @Test
    public void testOnTrimMemoryCompleteEvictsAllEntries() {
        when(mPackageOne.getSize()).thenReturn(25);
        when(mPackageTwo.getSize()).thenReturn(25);
        mLruPackageCache.put(Content.ImportRef.create("p", "1.0"), mPackageOne);
        mLruPackageCache.put(Content.ImportRef.create("p", "1.1"), mPackageTwo);

        mLruPackageCache.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);

        assertNull(mLruPackageCache.get(Content.ImportRef.create("p", "1.0")));
        assertNull(mLruPackageCache.get(Content.ImportRef.create("p", "1.1")));
    }
}
