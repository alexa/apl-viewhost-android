package com.amazon.apl.android.metrics.impl;

import com.amazon.apl.android.metrics.IMetricsRecorder;

import java.util.Map;

/**
 * The APLMetricsRecorder is an abstract class that defines methods to be implemented by MetricsRecorder
 * implementations
 */
public abstract class APLMetricsRecorder implements IMetricsRecorder {
    /**
     * Record a {@link com.amazon.apl.android.metrics.MetricEventType#SEGMENT_START} for a segment
     * @param name
     * @param metadata
     * @return the metricId for the emmitted {@link com.amazon.apl.android.metrics.MetricsEvent}
     */
    abstract long recordSegmentStart(String name, Map<String, String> metadata);

    /**
     * Record a {@link com.amazon.apl.android.metrics.MetricEventType#SEGMENT_END} for a segment
     * @param name
     * @param metadata
     */
    abstract void recordSegmentEnd(String name, Map<String, String> metadata, long segmentStartId);

    /**
     * Record a {@link com.amazon.apl.android.metrics.MetricEventType#SEGMENT_FAILED} for a segment
     * @param name
     * @param metadata
     */
    abstract void recordSegmentFailed(String name, Map<String, String> metadata, long segmentStartId);

    /**
     * Record a {@link com.amazon.apl.android.metrics.MetricEventType#COUNTER} for a metric with the amount
     * to change it by
     * @param name
     * @param amount
     */
    abstract void recordCounter(String name, long amount);
}
