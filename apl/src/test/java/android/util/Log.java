/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package android.util;

// Reference: https://stackoverflow.com/questions/36787449/how-to-mock-method-e-in-log
public class Log {
    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int INFO = 4;
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable e) {
        System.out.println("WARN: " + tag + ": " + msg + "\nCaused by " + e.getMessage());
        return 0;
    }

    public static int e(String tag, String msg) {
        System.out.println("ERROR: " + tag + ": " + msg);
        return 0;
    }
}