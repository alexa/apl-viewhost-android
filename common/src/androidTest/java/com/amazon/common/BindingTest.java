/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BindingTest {

    public class TestBoundObject extends BoundObject {
        public TestBoundObject() {
            bind(nTestBoundObjectCreate());
        }
        private native long nTestBoundObjectCreate();
    }

    private static final String TAG = "BindingTest";

    static {
        System.loadLibrary("common-jni");
    }

    private TestBoundObject mTestBoundObject;

    // Somewhere, there's tests that leak bound objects, so the floor of these tests
    // is not zero. We need to determine what the actual floor is before starting.
    private int mFloor;

    @Before
    public void createBoundObject() {
        runMemoryCleanup();
        NativeBinding.doDeletes();
        mFloor = NativeBinding.testBoundObjectCount();
        mTestBoundObject = new TestBoundObject();
    }

    @After
    public void cleanupTest() {
        mTestBoundObject = null;
        runMemoryCleanup();
        NativeBinding.doDeletes();
    }

    /**
     * Test that a native handle is assigned to te BoundObject.
     */
    @Test
    @SmallTest
    public void test_NativeOwner() {
        long handle = mTestBoundObject.getNativeHandle();
        assertTrue(NativeBinding.testNativePeer(handle));
        assertEquals(1, NativeBinding.testReferenceCount(handle));
    }


    @Test
    @SmallTest
    public void test_Bind() {
        long handle = mTestBoundObject.getNativeHandle();

        // The object has a native handle
        assertTrue("Expected object to have native handle", handle != 0);
        // The native handle is recognized as a binding
        assertTrue("Expected the native handle represents binding", mTestBoundObject.isBound());
        // The object is registered with the PhantomReference queue
        assertTrue("Expected bound object to be registered", NativeBinding.testBound(handle));
        // A single object is bound
        assertEquals(1 + mFloor, NativeBinding.testBoundObjectCount());
    }


    @Test
    @SmallTest
    public void test_Unbind() {
        long handle = mTestBoundObject.getNativeHandle();
        // free the memory
        mTestBoundObject = null;
        runMemoryCleanup();

        NativeBinding binding = NativeBinding.testPopPendingDelete();
        assertNotNull("Expected pending binding delete", binding);
        assertEquals("Expected test object pending delete", binding.getNativeHandle(), handle);
        NativeBinding.testUnBind(binding);
        assertFalse("Expected object to be unbound", NativeBinding.testBound(handle));
        assertEquals("Expected zero bound objects", 0 + mFloor, NativeBinding.testBoundObjectCount());

        //TODO test native free
    }


    @Test
    @SmallTest
    public void test_doDeletes() {
        long handle = mTestBoundObject.getNativeHandle();
        // free the memory
        mTestBoundObject = null;
        runMemoryCleanup();

        NativeBinding.doDeletes();
        assertFalse("Expected object to be unbound", NativeBinding.testBound(handle));
        assertEquals("Expected zero bound objects", 0 + mFloor, NativeBinding.testBoundObjectCount());

        //TODO test native free
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
