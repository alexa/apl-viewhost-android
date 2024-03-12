/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.error;

public final class DTException extends Exception {
    private final int mId;
    private final int mCode;

    public DTException(int id, int code, String message) {
        super(message);
        mId = id;
        mCode = code;
    }

    public DTException(int id, int code, String message, Throwable cause) {
        super(message, cause);
        mId = id;
        mCode = code;
    }

    public int getId() {
        return mId;
    }

    public int getCode() {
        return mCode;
    }
}
