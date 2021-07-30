/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;


import com.amazon.apl.android.APLBinding;
import com.amazon.apl.android.scaling.Scaling;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Vector;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BindingTest {

    private static final String TAG = "BindingTest";

    static {
        System.loadLibrary("apl-jni");
    }


    // Use Metrics as it the simplest of BoundObjects
    private Scaling mScaling;

    @Before
    public void createBoundObject() {
        APLBinding.doDeletes();
        mScaling = new Scaling(1.0, new Vector<>());
    }

    @After
    public void cleanupTest() {
        mScaling = null;
        runMemoryCleanup();
        APLBinding.doDeletes();
    }

    /**
     * Test that a native handle is assigned to te BoundObject.
     */
    @Test
    @SmallTest
    public void test_NativeOwner() {
        long handle = mScaling.getNativeHandle();
        assertTrue(APLBinding.testNativePeer(handle));
        assertEquals(1, APLBinding.testReferenceCount(handle));
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
