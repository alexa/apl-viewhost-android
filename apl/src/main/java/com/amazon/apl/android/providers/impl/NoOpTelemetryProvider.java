/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import com.amazon.apl.android.providers.ITelemetryProvider;

import java.util.concurrent.TimeUnit;

/**
 * NoOp implementation of a telemetry provider, discards all attempts to record telemetry.
 */
public final class NoOpTelemetryProvider implements ITelemetryProvider {

    private NoOpTelemetryProvider() {
        // enforce singleton instance
    }

    @Override
    public int createMetricId(String domain, String metricName, Type type) {
        return 0;
    }

    @Override
    public int getMetricId(String domain, String metricName) {
        return 0;
    }

    @Override
    public void startTimer(int metricId) {
    }

    @Override
    public void startTimer(int metricId, TimeUnit timeUnit, long elapsedTime) {
    }

    @Override
    public void stopTimer(int metricId) {
    }

    @Override
    public void fail(int metricId) {
    }

    @Override
    public void incrementCount(int metricId) {
    }

    @Override
    public void incrementCount(int metricId, int by) {
    }

    @Override
    public void onDocumentFinish() {
    }

    private static NoOpTelemetryProvider mSingletonInstance;

    public static NoOpTelemetryProvider getInstance() {
        if (mSingletonInstance == null) {
            mSingletonInstance = new NoOpTelemetryProvider();
        }
        return mSingletonInstance;
    }
}
