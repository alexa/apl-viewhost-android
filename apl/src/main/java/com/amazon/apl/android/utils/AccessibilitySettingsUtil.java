/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.utils;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.lang.reflect.Method;

/**
 * Provides facilities for querying the accessibility state of the system.
 */
public class AccessibilitySettingsUtil {

    private static final String TAG = "AccessibilitySttngsUtl";
    private static final AccessibilitySettingsUtil INSTANCE = new AccessibilitySettingsUtil();

    private static Method sIsHighTextContrastEnabledMethod;

    static {
        extractHighContrastMethod();
    }

    @VisibleForTesting
    AccessibilitySettingsUtil() {
    }

    public static AccessibilitySettingsUtil getInstance() {
        return INSTANCE;
    }

    /**
     * @param context The Android context.
     * @return Current user preference for the scaling factor for fonts.
     */
    public float getFontScale(Context context) {
        return context.getResources().getConfiguration().fontScale;
    }

    /**
     * @param context The Android context.
     * @return Returns if the high text contrast in the system is enabled.
     *
     * Credits: https://stackoverflow.com/questions/37422895/how-to-detect-accessibility-settings-on-android-is-enabled-disabled/37444704#37444704
     */
    public boolean isHighContrast(Context context) {
        AccessibilityManager am = getAccessibilityManager(context);
        if (am == null) {
            return false;
        }

        try {
            Object result = sIsHighTextContrastEnabledMethod.invoke(am);
            if (result != null && result instanceof Boolean)  {
                return (Boolean)result;
            }
        } catch (Exception e) {
            Log.e(TAG, "isHighTextContrastEnabled invoked with an exception", e);
        }
        return false;
    }

    /**
     * @param context  The Android context.
     * @return True screen reader is enabled, false otherwise.
     */
    public boolean isScreenReaderEnabled(Context context) {
        AccessibilityManager am = getAccessibilityManager(context);
        return (am == null) ? false : am.isEnabled();
    }

    private AccessibilityManager getAccessibilityManager(Context context) {
        Object service = context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (service instanceof AccessibilityManager) {
            return (AccessibilityManager) service;
        }
        return null;
    }

    private static void extractHighContrastMethod() {
        Class clazz = AccessibilityManager.class;
        try {
            sIsHighTextContrastEnabledMethod = clazz.getMethod("isHighTextContrastEnabled");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "isHighTextContrastEnabled not found in AccessibilityManager");
        }
    }
}
