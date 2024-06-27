/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.performance;

import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.MetricsEvent;
import com.amazon.apl.android.utils.MetricInfo;
import java.util.ArrayList;
import java.util.List;

public class NoOpDTMetricSink implements IMetricsSink, IMetricsService {
    @Override
    public void metricPublished(MetricsEvent event) {
    }

    @Override
    public List<MetricInfo> retrieveMetrics() {
        return new ArrayList<>();
    }

    @Override
    public void clearMetrics() {
    }
}
