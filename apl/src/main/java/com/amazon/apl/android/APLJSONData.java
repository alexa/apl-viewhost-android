/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import java.nio.charset.StandardCharsets;

/**
 * The JSONData class is a wrapper class that indicates the content is UTF8-encoded JSON,
 * instead of a standard Android string.
 */
public class APLJSONData {

    /**
     * Construct JSONData from a Java string
     * @param inString The string to parse
     * @return The JSONData
     */
    static public APLJSONData create(String inString) {
        return new APLJSONData(inString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Construct JSONData from a byte array of raw UTF8 data.  No verification is performed.
     * @param utf8 The raw data
     */
    private APLJSONData(byte[] utf8) {
        mBytes = utf8;
    }

    @SuppressWarnings("UnusedDeclaration")
    private byte[] mBytes;
}
