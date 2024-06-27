/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.devtools.models.performance;

import android.os.Handler;

import androidx.annotation.VisibleForTesting;
import com.amazon.apl.android.metrics.APLWellknownMetricsSink;
import com.amazon.apl.android.metrics.MetricsEvent;
import com.amazon.apl.android.utils.MetricInfo;
import com.amazon.apl.devtools.util.DependencyContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dev Tools Metric Sink to be able to retrieve the capture performance metrics
 * to report back to the Dev Tool client.
 */
public class DTMetricsSink extends APLWellknownMetricsSink implements IMetricsService {
    private final static String TAG = DTMetricsSink.class.getSimpleName();
    private final Handler mDTHandler;
    private final List<MetricInfo> mCompletedMetrics;
    public DTMetricsSink() {
        this(DependencyContainer.getInstance()
                .getTargetCatalog().getHandler());
    }

    @VisibleForTesting
    DTMetricsSink(final Handler handler) {
        // TODO get seed from runtime
        super(-1);
        mDTHandler = handler;
        mCompletedMetrics = new ArrayList<>();
    }

    @Override
    public void metricPublished(final MetricsEvent event) {
        mDTHandler.post(()->super.metricPublished(event));
    }

    @Override
    public void timerPublished(String name, long duration, Map<String, String> metadata) {
        double durationInSeconds = (double) duration/1_000_000_000;
        mCompletedMetrics.add(new MetricInfo(name, durationInSeconds));
    }

    @Override
    public void counterPublished(String name, long value, Map<String, String> metadata) {
        mCompletedMetrics.add(new MetricInfo(name, value));
    }

    @Override
    public void segmentPublished(String name, long duration, Map<String, String> metadata) {
        double durationInSeconds = (double) duration/1_000_000_000;
        mCompletedMetrics.add(new MetricInfo(name, durationInSeconds));
    }

    @Override
    public List<MetricInfo> retrieveMetrics() {
        logAndReset();
        return mCompletedMetrics;
    }

    @Override
    public void clearMetrics() {
        mCompletedMetrics.clear();
    }
}
