/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

import com.amazon.apl.android.IDocumentLifecycleListener;

/**
 *  A MetricsSink is the destination where metrics are emitted to. Runtimes are expected to implement a sink
 * 	and then publish the metrics to somewhere usable for analysis.
 */
public interface IMetricsSink extends IDocumentLifecycleListener {
    void metricPublished(MetricsEvent event);
}
