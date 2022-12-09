/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import androidx.test.filters.SmallTest;

import com.amazon.apl.android.ExtensionCommandDefinition;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExtensionCommandDefinitionTest extends ViewhostRobolectricTest {

    @Test
    @SmallTest
    public void test_commandDefEmpty() {

        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:TEST", "MyFooCommand");

        assertEquals("MyFooCommand", def.getName());
        assertEquals("aplext:TEST", def.getURI());
        assertFalse(def.getAllowFastMode());
        assertFalse(def.getRequireResolution());
        assertEquals(0, def.getPropertyCount());
    }



    @Test
    @SmallTest
    public void test_commandDefSimple() {
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test","MyFooCommand")
                .allowFastMode(true)
                .requireResolution(true)
                .property("width", 100, false)
                .property("height", 120, true);

        assertEquals("MyFooCommand", def.getName());
        assertEquals("aplext:Test", def.getURI());
        assertTrue(def.getAllowFastMode());
        assertTrue(def.getRequireResolution());
        assertEquals(2, def.getPropertyCount());
        int w = def.<Integer>getPropertyValue("width");
        assertEquals(100, w);
        int h = def.<Integer>getPropertyValue("height");
        assertEquals(120, h);
        assertTrue(def.isPropertyRequired("height"));
    }


    @Test
    @SmallTest
    public void test_commandDefIllegal() {
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test","MyFooCommand")
                .property("type", 100, false)
                .property("when", false, false);

        assertEquals(0, def.getPropertyCount());

        //TODO ASSERT_TRUE(LogMessage());
    }


    @Test
    @SmallTest
    public void test_commandMissingProperty() {
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyFooCommand")
                .allowFastMode(true)
                .requireResolution(true);

        assertEquals("MyFooCommand", def.getName());
        assertEquals("aplext:Test", def.getURI());
        assertTrue(def.getAllowFastMode());
        assertTrue(def.getRequireResolution());
        assertEquals(0, def.getPropertyCount());
        assertNull(def.<Integer>getPropertyValue("foo"));
        assertNull(def.<Integer>getPropertyValue("bar"));
    }


}





