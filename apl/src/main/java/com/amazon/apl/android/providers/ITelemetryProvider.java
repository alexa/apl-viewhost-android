/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.IDocumentLifecycleListener;

import java.util.concurrent.TimeUnit;

/**
 * Interface for tracking Metrics from the APL Android Viewhost.
 */
public interface ITelemetryProvider extends IDocumentLifecycleListener {

    enum Type {COUNTER, TIMER}

    String APL_DOMAIN = BuildConfig.DEBUG
            ? "APL-Android-Debug" : "APL-Android";

    String RENDER_DOCUMENT = "renderDocument";
    String RESTORE_DOCUMENT = "restoreDocument";
    String LIBRARY_INITIALIZATION_FAILED = "libraryInitializationFailed";
    String FAIL_SUFFIX = ".fail";

    int UNKNOWN_METRIC_ID = -1;

    /**
     * Identify a metric.
     *
     * @param domain     The metric domain.
     * @param metricName The unique metric name.
     * @return GUID representing this metric.
     */
    int createMetricId(String domain, String metricName, Type type);

    /**
     * Get the metric identifier.  The metric should have previously been created.
     *
     * @param domain     The metric domain.
     * @param metricName The unique metric name.
     * @return GUID representing this metric, or {@link ITelemetryProvider#UNKNOWN_METRIC_ID} if the metric doesn't exist.
     */
    int getMetricId(String domain, String metricName);

    /**
     * Start a timer for a metric
     *
     * @param metricId The metric identifier.
     */
    void startTimer(int metricId);

    /**
     * Start a timer for a metric, with an initial seeded elapsed time value. For example,
     * a timer may be started with an elapsed time of 10sec.
     *
     * @param metricId    The metric identifier.
     * @param timeUnit    The time unit of the elapsed time.
     * @param elapsedTime The elapsed time.
     */
    void startTimer(int metricId, TimeUnit timeUnit, long elapsedTime);

    /**
     * End a timer for a metric.
     *
     * @param metricId The metric identifier..
     */
    void stopTimer(int metricId);

    /**
     * Cancels an active metric timer.
     *
     * @param metricId The metric identifier.
     */
    void fail(int metricId);

    /**
     *
     * Increment the metric counter.
     *
     * @param metricId The metric identifier.
     */
    void incrementCount(int metricId);

    /**
     *
     * Increment the metric counter by a given value.
     *
     * @param metricId The metric identifier.
     * @param by The counter counter increment.
     */
    void incrementCount(int metricId, int by);
}
