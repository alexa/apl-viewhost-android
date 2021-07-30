/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

/**
 * Exception to wrap {@link OutOfMemoryError}.
 */
public class BitmapCreationException extends Exception {
    BitmapCreationException(String message, Throwable e) {
        super(message, e);
    }
}
