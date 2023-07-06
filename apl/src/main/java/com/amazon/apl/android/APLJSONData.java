/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.util.Log;
import com.amazon.common.BoundObject;

/**
 * The JSONData class is a wrapper class that indicates the content is JSON,
 * instead of a standard Android string.
 */
public class APLJSONData extends BoundObject {

    private final int mSize;

    /**
     * Construct JSONData from a Java string
     * @param inString The string to parse
     * @return The JSONData
     */
    static public APLJSONData create(String inString) {
        return new APLJSONData(inString);
    }

    static public APLJSONData create(byte[] utf8) {
        return new APLJSONData(utf8);
    }

    /**
     * Construct JSONData from a String.
     */
    private APLJSONData(String data) {
        long handle = nCreate(data);
        bind(handle);
        mSize = data.length();
    }

    /**
     * Construct JSONData from a byte array of raw UTF8 data.  No verification is performed.
     * @param utf8 The raw data
     */
    private APLJSONData(byte[] utf8) {
        long handle = nCreateWithByteArray(utf8);
        bind(handle);
        mSize = utf8.length;
    }

    /**
     * @return the size of the String used to create this JsonData.
     */
    public int getSize() {
        return mSize;
    }

    private static native long nCreate(String data);

    private static native long nCreateWithByteArray(byte[] byteArray);
}
