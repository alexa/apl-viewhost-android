/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

public class JNIUtilsTest {

    @Test
    public void testPositiveHandles() {
        assertTrue( JNIUtils.isHandleValid(1L));
    }

    @Test
    public void testNegativeHandles() {
        assertTrue( JNIUtils.isHandleValid(-1L));
    }

    @Test
    public void testNullHandles() {
        assertFalse( JNIUtils.isHandleValid(0L));
    }

}
