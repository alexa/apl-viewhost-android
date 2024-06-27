/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.providers.impl;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.utils.MetricInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simple Telemetry provider that logs results when document is complete.
 */
public class LoggingTelemetryProvider implements ITelemetryProvider {

    private static final String TAG = "TelemetryReport";
    private static final String COUNT_LOG = " - success:";
    private static final String FAIL_LOG = "  fail:";
    private static final String TIMER_AVG_LOG = "  average:";
    private static final String TIMER_TOTAL_LOG = "  total:";
    private static final String MS_LOG = "ms";

    // collection of metrics in use
    private final List<Metric> mMetrics = Collections.synchronizedList(new ArrayList<>());
    private final List<String> mIds = Collections.synchronizedList(new ArrayList<>());

    @Override
    public synchronized int createMetricId(String domain, String metricName, Type type) {
        // Add an identifier to the known identifier list and
        // return the location in the metrics collection
        Metric metric = new Metric();
        metric.metricName = idOf(domain, metricName);
        int index = mMetrics.size();
        mMetrics.add(metric);
        mIds.add(index, metric.metricName);
        return index;
    }

    @Override
    public synchronized int getMetricId(String domain, String metricName) {
        return mIds.indexOf(idOf(domain, metricName));
    }

    /**
     * Creates a string identifier for a metric domain and name.
     *
     * @param domain     The metric domain.
     * @param metricName The metric name.
     * @return A string in the format "domain.name".
     */
    private String idOf(String domain, String metricName) {
        return domain + "." + metricName;
    }

    @Override
    public synchronized void reportTimer(int metricId, TimeUnit timeUnit, long time) {
        Metric metric = mMetrics.get(metricId);
        metric.totalTime = timeUnit.toNanos(time); // Convert to nanos
        final long totalTimeMillis = TimeUnit.MILLISECONDS.convert(metric.totalTime,
                TimeUnit.NANOSECONDS);
        // This will log a line in Logcat of the form:
        // I/TelemetryReport: Recording timer: APL-Android-Debug.renderDocument - total:61ms
        Log.i(TAG, String.format("Recording timer: %s -%s%d%s", metric.metricName, TIMER_TOTAL_LOG,
                totalTimeMillis, MS_LOG));
        metric.success++;
        metric.seedTime = 0;
        metric.startTime = 0; // reset time
    }

    /**
     * Starts a timer and increments the attempt success. If the timer
     * is already running this method has no effect.
     *
     * @param metricId The metric identifier.
     */
    @Override
    public synchronized void startTimer(int metricId) {
        startTimer(metricId, TimeUnit.NANOSECONDS, 0);
    }

    /**
     * Start a timer for a metric, with an initial seeded elapsed time value. For example,
     * a timer may be started with an elapsed time of 10sec.
     *
     * @param metricId           The metric identifier.
     * @param timeUnit           The time unit of the elapsed time.
     * @param initialElapsedTime The initial elapsed time.
     */
    @Override
    public synchronized void startTimer(int metricId, TimeUnit timeUnit, long initialElapsedTime) {
        Metric metric = mMetrics.get(metricId);

        if (metric.startTime == 0) {
            metric.startTime = realtimeNanos();
            metric.seedTime = timeUnit.toNanos(initialElapsedTime);
        }
    }

    /**
     * Stops a running timer and adds the elapsed time to the total time. If the
     * timer was not running, this method has no effect.
     *
     * @param metricId The metric identifier.
     */
    @Override
    public synchronized void stopTimer(int metricId) {
        long endTime = realtimeNanos();
        stopTimer(metricId, TimeUnit.NANOSECONDS, endTime);
    }

    /**
     * End a timer for a metric.
     *
     * @param metricId  The metric identifier.
     * @param timeUnit  The time unit of the time the timer should be stopped at.
     * @param endTime   The time to stop the timer at.
     */
    @Override
    public synchronized void stopTimer(int metricId, TimeUnit timeUnit, long endTime) {
        Metric metric = mMetrics.get(metricId);
        if (metric.startTime != 0) {
            metric.totalTime += timeUnit.toNanos(endTime) - (metric.startTime - metric.seedTime);
            metric.success++;
            final long totalTimeMillis = TimeUnit.MILLISECONDS.convert(metric.totalTime,
                    TimeUnit.NANOSECONDS);
            // This will log a line in Logcat of the form:
            // I/TelemetryReport: Stopping timer: APL-Android-Debug.renderDocument - total:61ms
            Log.i(TAG, String.format("Stopping timer: %s -%s%d%s", metric.metricName, TIMER_TOTAL_LOG,
                    totalTimeMillis, MS_LOG));

            metric.seedTime = 0;
            metric.startTime = 0; // reset time
        }
    }

    /**
     * Fails an active metric timer, increments the fail success, and
     * stops the timer.
     *
     * @param metricId The metric identifier.
     */
    @Override
    public synchronized void fail(int metricId) {
        Metric metric = mMetrics.get(metricId);
        metric.fail++;
        metric.startTime = 0;  // end timer
        Log.i(TAG, String.format("Incrementing counter: %s by 1 -%s%d", metric.metricName, FAIL_LOG,
                metric.fail));
    }

    /**
     * Increments the use success of a metric.
     *
     * @param metricId The metric identifier.
     */
    @Override
    public synchronized void incrementCount(int metricId) {
        incrementCount(metricId, 1);
    }

    @Override
    public synchronized void incrementCount(int metricId, int by) {
        Metric metric = mMetrics.get(metricId);
        metric.success += by;
        Log.i(TAG, String.format("Incrementing counter: %s by %d%s%d", metric.metricName, by, COUNT_LOG,
                metric.success));
    }

    /**
     * The document is no longer valid for display.
     */
    @Override
    public void onDocumentFinish() {
        logAndResetMetrics();
    }

    /**
     * Log metrics to standard out and reset all values.
     */
    @SuppressLint("DefaultLocale")
    private void logAndResetMetrics() {
        for (int i = 0; i < mMetrics.size(); i++) {
            StringBuilder builder = new StringBuilder();

            Metric metric = mMetrics.get(i);
            // add use data
            builder.append(metric.metricName)
                    .append(COUNT_LOG).append(metric.success)
                    .append(FAIL_LOG).append(metric.fail);
            // add timer data if any
            if (metric.totalTime > 0 && metric.success > 0) {
                long avgTime = TimeUnit.MILLISECONDS.convert(
                        Math.round(metric.totalTime / (float) metric.success),
                        TimeUnit.NANOSECONDS);
                long totalTime = TimeUnit.MILLISECONDS.convert(metric.totalTime,
                        TimeUnit.NANOSECONDS);
                builder.append(TIMER_TOTAL_LOG).append(totalTime).append(MS_LOG);
                builder.append(TIMER_AVG_LOG).append(avgTime).append(MS_LOG);
                metric.totalTime = 0;
            }
            metric.success = 0;
            metric.fail = 0;

            Log.i(TAG, builder.toString());
        }

        // Clear metrics
        mMetrics.clear();
        mIds.clear();
    }

    @VisibleForTesting
    public long realtimeNanos() {
        return SystemClock.elapsedRealtimeNanos();
    }

    /**
     * Simplistic Metric class that tracks success and average time for success.
     */
    public class Metric {
        public String metricName;
        public long seedTime = 0;
        public long startTime = 0;
        public long totalTime = 0;
        public int success = 0;
        public int fail = 0;
    }

    /**
     * Get the simple metric model associated with the metric Id.
     *
     * @param id The metric Id obtained from {@link #getMetricId(String, String)}.
     * @return the metric model.
     */
    public Metric getMetric(int id) {
        return mMetrics.get(id);
    }

    /**
     * Method that returns all timer related metrics. The value of each metric should be in Milliseconds.
     *
     * @return A Thread safe List of performance metrics.
     */
    public synchronized List<MetricInfo> getPerformanceMetrics() {
        List<MetricInfo> copyOfMetrics = Collections.synchronizedList(new ArrayList<>());
        for (Metric metric: mMetrics){
            MetricInfo metricInfo = new MetricInfo(metric.metricName, (double) TimeUnit.MILLISECONDS.convert(metric.totalTime, TimeUnit.NANOSECONDS));
            copyOfMetrics.add(metricInfo);
        }
        return copyOfMetrics;
    }
}
