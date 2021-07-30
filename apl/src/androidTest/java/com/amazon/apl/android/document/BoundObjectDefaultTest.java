/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.APLBinding;

import junit.framework.TestCase;

import org.junit.Test;

import androidx.test.filters.SmallTest;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * This class tests memory allocations and free.
 */
public interface BoundObjectDefaultTest {

    /**
     * Create a handle to the bound object under test.  The tests should not hold
     * a reference to the bound object to allow for gc and unbinding tests.
     * Recommended pattern for this method:
     * <p>
     * Foo foo =  Foo.create();
     * long handle = foo.getNativeHandle(); // do not in-line with return
     * return handle;
     *
     * @return The handle BoundObject under test.
     */
    long createBoundObjectHandle();


    /**
     * Test the allocation and free of an APL RootContext memory..
     */
    @Test
    @SmallTest
    default void testMemory_binding() {

        long handle = createBoundObjectHandle();

        // The object has a native handle
        assertTrue("Expected object to have native handle", handle != 0);
        // The object is registered with the PhantomReference queue
        TestCase.assertTrue("Expected bound object to be registered", APLBinding.testBound(handle));

        // memory free
        System.runFinalization();
        System.gc();
        try {
            // sleep to allow gc to keep pace with short lived test cases
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        APLBinding.doDeletes();

        assertFalse("expected unbound object", APLBinding.testBound(handle));
    }

    @Test
    @SmallTest
    default void testMemory_Handles() {

        long handle = createBoundObjectHandle();

        // Test the core construct and destruct count
        //TODO construction tests
    }

}
