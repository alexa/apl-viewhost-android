/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.enums;

/**
 * The different DT Network type events that may occur.
 */
public enum DTNetworkRequestType {
    PACKAGE("package"),
    IMAGE("image");

    private final String mType;
    DTNetworkRequestType(String type) {
        mType = type;
    }

    public String toString() {
        return mType;
    }
}
