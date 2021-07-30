/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

/**
 * This error should be thrown when a call is not on the Ui Thread when it should be
 */
public class NotOnUiThreadError extends Error {

    public NotOnUiThreadError(String message) {
        super(message);
    }
}
