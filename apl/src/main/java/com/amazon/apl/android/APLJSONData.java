/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

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

    /**
     * Construct JSONData from a String.
     */
    private APLJSONData(String data) {
        long handle = nCreate(data);
        bind(handle);
        mSize = data.length();
    }

    /**
     * @return the size of the String used to create this JsonData.
     */
    public int getSize() {
        return mSize;
    }

    private static native long nCreate(String data);
}
