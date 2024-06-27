/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

/**
 * Each MetricsEvent will have one of these types.
 */
public enum MetricEventType {
    // Counters have a positive or negative value that represents the current sum of the counter
    COUNTER,
    // The start of some measurable operation. The value is a timestamp.
    SEGMENT_START,
    // The end of some measurable operation. The value is a timestamp.
    SEGMENT_END,
    // The end of some measurable operation that failed. The value is a platform timestamp in milliseconds.
    SEGMENT_FAILED,
    // A significant event in the document lifecycle. The value is a timestamp.
    MILESTONE
}
