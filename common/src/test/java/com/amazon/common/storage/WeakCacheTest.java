/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.common.storage;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;

public class WeakCacheTest {
    private static class HeldClass {
        private int mMember;
    }

    private static class HoldingClass {
        private HeldClass mHeldClass;

        private HoldingClass(HeldClass heldClass) {
            mHeldClass = heldClass;
        }

        public void setHeldClass(HeldClass heldClass) {
            mHeldClass = heldClass;
        }
    }

    private WeakCache<String, HeldClass> mWeakCache;

    @Before
    public void setup() {
        mWeakCache = new WeakCache<>();
    }

    @Test
    public void testCleanUp() {
        HeldClass heldClass = new HeldClass();
        HoldingClass holdingClass = new HoldingClass(heldClass);
        mWeakCache.put("key", new WeakReference<>(heldClass));
        assertEquals(heldClass, mWeakCache.get("key"));
        holdingClass.setHeldClass(null);
        heldClass = null;
        runMemoryCleanup();
        assertNull(mWeakCache.get("key"));
    }

    private void runMemoryCleanup() {
        System.runFinalization();
        System.gc();
        try {
            // sleep to allow gc to keep pace with short lived test cases
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
