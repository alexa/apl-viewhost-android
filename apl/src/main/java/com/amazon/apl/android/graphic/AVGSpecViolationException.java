/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

/**
 * Exception thrown for AVG items which are not compliant with the specification.
 */
public class AVGSpecViolationException extends RuntimeException {
    public AVGSpecViolationException(String message) {
        super(message);
    }
}
