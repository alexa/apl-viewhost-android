/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;


import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class AplJniLoadInstrumentedTest {


    @Test
    @SmallTest
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.amazon.apl.android.test", appContext.getPackageName());
    }

    @Test
    @SmallTest
    public void libraryLoad() {
        try {
            System.loadLibrary("common-jni");
            System.loadLibrary("apl-jni");
            System.loadLibrary("discovery-jni");
        } catch (Exception e) {
            fail("APL JNI library could not be loaded");
        }
    }

}
