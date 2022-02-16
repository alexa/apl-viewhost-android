/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

public class APLViewhostTest {
    // Load the APL library.
    static {
        System.loadLibrary("common-jni");
        System.loadLibrary("apl-jni");
        System.loadLibrary("discovery-jni");
    }
}
