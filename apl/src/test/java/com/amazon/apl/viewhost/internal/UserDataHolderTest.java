/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.UserDataHolder;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UserDataHolderTest extends ViewhostRobolectricTest {
    @Test
    public void testBasicBehavior() {
        UserDataHolder holder = new UserDataHolder();
        assertFalse(holder.hasUserData());
        assertNull(holder.getUserData());

        String userData = "MyData";
        assertTrue(holder.setUserData(userData));
        assertTrue(holder.getUserData() instanceof String);
        assertTrue(holder.hasUserData());
        assertEquals("MyData", holder.getUserData());
    }
}
