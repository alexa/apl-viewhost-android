/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

import com.amazon.apl.android.BuildConfig;

import java.util.Map;

/**
 * The MetricsRecorder is the class responsible for recording and publishing metrics.
 * It should contain all the background threading logic as well as the business logic needed to calculate the WellKnown metrics.
 */
public interface IMetricsRecorder {

    String APL_DOMAIN = BuildConfig.DEBUG
            ? "APL-Android-Debug" : "APL-Android";
    // Add metadata that will be included with all metrics.
    // Will replace value if key already exists.
    void mergeMetadata(String key, String value);

    // Merges the MetricsRecorder's metadata dictionary with this one.
    // Will overwrite values if keys exist.
    void mergeMetadata(Map<String, String> metadata);

    // Create a counter object.
    ICounter createCounter(String counterName);

    // Records a milestone.
    void recordMilestone(String milestoneName);

    // Starts and records a timer object. When finished, the timer should be stopped.
    ITimer startTimer(String timerName, Map<String,String> metadata);

    // Adds a new sink to this recorder.
    void addSink(IMetricsSink sink);
}
