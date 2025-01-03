/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics.impl;

import com.amazon.apl.android.metrics.ICounter;

/**
 * The counter interface is capable of batching counter values to be recorded all at once later.
 * Otherwise, it just queues up the increment value to be processed immediately.
 */
public class Counter implements ICounter {
    private APLMetricsRecorder mMetricsRecorder;
    private String mName;

    Counter(String name, APLMetricsRecorder metricsRecorder) {
        mMetricsRecorder = metricsRecorder;
        mName = name;
    }

    @Override
    public void increment(long amount) {
        mMetricsRecorder.recordCounter(mName, amount);
    }
}
