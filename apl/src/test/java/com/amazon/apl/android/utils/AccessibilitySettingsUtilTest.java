/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.utils;

import android.content.res.Configuration;
import android.view.accessibility.AccessibilityManager;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

public class AccessibilitySettingsUtilTest extends ViewhostRobolectricTest {

    private AccessibilitySettingsUtil mUtil;

    @Before
    public void setup() {
        mUtil = new AccessibilitySettingsUtil();
    }

    @Test
    public void test_isScreenReaderEnabled_default() {
        assertFalse(mUtil.isScreenReaderEnabled(getApplication()));
    }

    @Test
    public void test_isScreenReaderEnabled_disabled() {
        shadowOf(getAccessibilityManager())
                .setEnabled(true);
        assertTrue(mUtil.isScreenReaderEnabled(getApplication()));
    }

    @Test
    public void test_getFontScale_default() {
        assertEquals(1.0f, mUtil.getFontScale(getApplication()), 0.01f);
    }

    @Test
    public void test_getFontScale_2x() {
        simulateFontScale2x();
        assertEquals(2.0f, mUtil.getFontScale(getApplication()), 0.01f);
    }

    @Test
    public void test_isHighContrast_default() {
        assertFalse(mUtil.isHighContrast(getApplication()));
    }

    @Test
    public void test_isHighContrast_enabled() {
        simulateHighContrast();
        assertTrue(mUtil.isHighContrast(getApplication()));
    }

    private void simulateFontScale2x() {
        Configuration c = new Configuration();
        c.fontScale = 2.0f;
        getApplication().getResources().getConfiguration().setTo(c);
    }

    /**
     * Simulates high contrast by reflection.
     * Credits: https://stackoverflow.com/questions/37402880/enabling-high-contrast-text-mode-from-within-android-java-using-accessibilitym
     *          https://github.com/ShadeWalker/Tango_AL813/blob/b50b1b7491dc9c5e6b92c2d94503635c43e93200/frameworks/base/core/java/android/view/accessibility/AccessibilityManager.java#L80
     */
    private void simulateHighContrast() {
        int STATE_FLAG_HIGH_TEXT_CONTRAST_ENABLED = 0x00000004;
        Class clazz = AccessibilityManager.class;
        try {
            Method m = clazz.getDeclaredMethod("setStateLocked", int.class);
            m.setAccessible(true);
            m.invoke(getAccessibilityManager(), STATE_FLAG_HIGH_TEXT_CONTRAST_ENABLED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AccessibilityManager getAccessibilityManager() {
        return (AccessibilityManager) getApplication().getSystemService(ACCESSIBILITY_SERVICE);
    }
}
