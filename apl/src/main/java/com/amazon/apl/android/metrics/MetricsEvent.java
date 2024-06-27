/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

import com.google.auto.value.AutoValue;

import java.util.Map;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * This is the class that is published to the metrics sinks. It's purely used to hold the data and contains no logic.
 */
@AutoValue
public abstract class MetricsEvent {
    // The id of this event. Used for relational purposes.
    // Should be unique within a root MetricsRecorder.
    public abstract long getMetricId();

    // The name of this metric.
    @NonNull
    public abstract String getMetricName();

    // The type of metric event this is.
    @NonNull
    public abstract MetricEventType getEventType();

    // The snapshot of metadata at the time of recording this metric.
    @Nullable
    public abstract Map<String, String> getMetaData();

    // The optional id of the related metric. Only used and required when this is a SegmentEnd.
    public abstract long getRelateId();

    // Value depends on what the type is. See enum comments.
    public abstract long getValue();

    public static Builder builder() {
        return new AutoValue_MetricsEvent.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder metricId(long metricId);
        public abstract Builder metricName(String metricName);
        public abstract Builder eventType(MetricEventType eventType);
        public abstract Builder metaData(Map<String, String> metaData);
        public abstract Builder relateId(long relateId);
        public abstract Builder value(long value);
        public abstract MetricsEvent build();
    }
}
