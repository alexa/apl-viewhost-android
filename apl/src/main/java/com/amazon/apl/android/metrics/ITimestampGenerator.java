/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

/**
 * To be used for timestamp related logic.
 */
public interface ITimestampGenerator {
    // Generates a monotonically increasing timestamp.
    long generateTimeStamp();
}
