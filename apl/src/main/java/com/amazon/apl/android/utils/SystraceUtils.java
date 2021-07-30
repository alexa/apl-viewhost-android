/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.os.Trace;

import com.amazon.apl.android.BuildConfig;

import java.lang.reflect.Method;

public class SystraceUtils {
    private SystraceUtils() {
        // Do nothing.
    }

    public static void startTrace(String className, String methodName) {
        if(BuildConfig.DEBUG) {
            Trace.beginSection(className + "." + methodName);
        }
    }

    public static void endTrace() {
        if(BuildConfig.DEBUG) {
            Trace.endSection();
        }
    }
}
