/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SettingsDocumentTest extends AbstractDocViewTest  {
    private static final String DUMMY_DOC = "\"type\": \"Text\"";
    private static final String SETTINGS_DOC =
            "\"settings\": {\n" +
            "    \"propertyA\": 180000,\n" +
            "    \"-propertyB\": true,\n" +
            "    \"-propertyC\": \"abc\",\n" +
            "    \"-propertyD\": 3.1416\n" +
            "}";

    private static final String SUB_SETTINGS_DOC =
            "\"settings\": {\n" +
            "  \"settingA\": {" +
            "    \"propertyA\": true,\n" +
            "    \"-propertyB\": 12\n" +
            "  }\n" +
            "}";

    private static final String EMPTY_SETTINGS_DOC =
            "\"settings\": {\n" +
                    "}";

    /**
     * Test to valid that properties found, should receive the expected value.
     */
    @Test
    public void testGetSetting_PropertiesFoundCase() {
        inflate(DUMMY_DOC, SETTINGS_DOC);

        assertTrue(mAPLController.hasSetting("propertyA"));
        assertTrue(mAPLController.hasSetting("-propertyB"));
        assertTrue(mAPLController.hasSetting("-propertyC"));
        assertTrue(mAPLController.hasSetting("-propertyD"));

        int a = mAPLController.optSetting("propertyA", 0);
        boolean b = mAPLController.optSetting("-propertyB", false);
        String c = mAPLController.optSetting("-propertyC", "");
        double d = mAPLController.optSetting("-propertyD", 0.0);

        assertEquals(180000, a);
        assertTrue(b);
        assertEquals("abc", c);
        assertEquals(3.1416, d, 0.001);
    }

    /**
     * Test to valid that properties not found, should always fallback.
     */
    @Test
    public void testGetSetting_PropertiesNotFoundCase() {
        inflate(DUMMY_DOC, EMPTY_SETTINGS_DOC);

        assertFalse(mAPLController.hasSetting("propertyA"));
        assertFalse(mAPLController.hasSetting("-propertyB"));
        assertFalse(mAPLController.hasSetting("-propertyC"));
        assertFalse(mAPLController.hasSetting("-propertyD"));

        int a = mAPLController.optSetting("propertyA", 0);
        boolean b = mAPLController.optSetting("-propertyB", false);
        String c = mAPLController.optSetting("-propertyC", "");
        double d = mAPLController.optSetting("-propertyD", 0.0);

        assertEquals(0, a);
        assertFalse(b);
        assertEquals("", c);
        assertEquals(0.0, d, 0.001);
    }

    /**
     * Test to valid if APL writer has specified a wrong datatype, APL should not crash and
     * must return the fallback value.
     */
    @Test
    public void testGetSetting_PropertiesFoundWithWrongDataTypeCase() {
        inflate(DUMMY_DOC, SETTINGS_DOC);

        assertTrue(mAPLController.hasSetting("propertyA"));
        assertTrue(mAPLController.hasSetting("-propertyB"));
        assertTrue(mAPLController.hasSetting("-propertyC"));
        assertTrue(mAPLController.hasSetting("-propertyD"));

        boolean a = mAPLController.optSetting("propertyA", false);
        int b = mAPLController.optSetting("-propertyB", 0);
        double c = mAPLController.optSetting("-propertyC", 0.0);
        String d = mAPLController.optSetting("-propertyD", "");

        assertFalse(a);
        assertEquals(0, b);
        assertEquals(0.0, c, 0.001);
        assertEquals("", d);
    }

    /**
     * Test to valid if APL writer has specified a wrong datatype, APL should not crash and
     * must return the fallback value.
     */
    @Test
    public void testGetSetting_Subsettings() {
        inflate(DUMMY_DOC, SUB_SETTINGS_DOC);

        assertTrue(mAPLController.hasSetting("settingA"));
        Map<String, Object> subSettings = mAPLController.optSetting("settingA", new HashMap<>());
        boolean a = (boolean) subSettings.get("propertyA");
        int b = (int) subSettings.get("-propertyB");

        assertTrue(a);
        assertEquals(12, b);
    }
}
