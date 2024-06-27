/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

/**
 *  Counters will emit a MetricsEvent each time increment is called.
 *  Runtimes may be interested in aggregating all the counter increments within their sink and then sending them up as one.
 */
public interface ICounter {
    // Increments a counter by the specified amount.
    void increment(long amount);
}
