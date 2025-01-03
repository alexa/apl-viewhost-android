/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics.impl;

import android.util.Log;

import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.ITimestampGenerator;
import com.amazon.apl.android.metrics.MetricEventType;
import com.amazon.apl.android.metrics.MetricsEvent;
import com.amazon.apl.android.thread.IHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The MetricsRecorder is the class responsible for recording and publishing metrics.
 * It should contain all the background threading logic as well as the business logic needed to calculate the WellKnown metrics.
 */
public class MetricsRecorder extends APLMetricsRecorder {

    private static final String TAG = "MetricsRecorder";

    private List<IMetricsSink> mSinkList;
    private Map<String, String> mMetadata;
    private ITimestampGenerator mTimestampGenerator;
    private static AtomicLong mMetricId = new AtomicLong(1);
    private IHandler mHandler;


    /**
     * To be used by the runtime
     */
    public MetricsRecorder() {
        mSinkList = new ArrayList<>();
        mMetadata = new HashMap<>();
        mTimestampGenerator = new TimestampGenerator();
        mHandler = MetricsHandler.getInstance();
    }

    //package private constructor For testing only
    MetricsRecorder(final ITimestampGenerator timestampGenerator, final IHandler handler) {
        mSinkList = new ArrayList<>();
        mMetadata = new HashMap<>();
        mTimestampGenerator = timestampGenerator;
        mHandler = handler;
    }

    @Override
    public void mergeMetadata(String key, String value) {
        mMetadata.put(key, value);
    }

    @Override
    public void mergeMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return;
        }
        Log.d(TAG, "meta data merged");
        mMetadata.putAll(metadata);
    }

    @Override
    public ICounter createCounter(String counterName) {
        return new Counter(counterName, this);
    }

    @Override
    public void recordMilestone(String milestoneName) {
        long timeStamp = mTimestampGenerator.generateTimeStamp();
        post(() -> {
            long metricId = mMetricId.getAndIncrement();
            MetricsEvent event = MetricsEvent.builder()
                    .metricId(metricId)
                    .metricName(milestoneName)
                    .eventType(MetricEventType.MILESTONE)
                    .relateId(0)
                    .value(timeStamp)
                    .metaData(cloneMetaData())
                    .build();
            recordMetric(event);
        });
    }

    /**
     * Creates a snapshot of metatadata.
     *
     * @return map
     */
    private HashMap<String, String> cloneMetaData() {
        HashMap<String, String> clone = new HashMap<>();
        for(Map.Entry<String, String> entry: mMetadata.entrySet()) {
            String key = entry.getKey();
            clone.put(key, entry.getValue());
        }
        return clone;
    }

    @Override
    public ITimer startTimer(String timerName, Map<String, String> metadata) {
        HashMap<String, String> mergedMap = new HashMap<>();
        mergedMap.putAll(mMetadata);
        mergedMap.putAll(metadata == null ? new HashMap<>() : metadata);
        return new Timer(timerName, mergedMap, this);
    }

    @Override
    public void addSink(IMetricsSink sink) {
        mSinkList.add(sink);
    }

    private void recordMetric(MetricsEvent event) {
        for (IMetricsSink sink: mSinkList) {
            sink.metricPublished(event);
        }
    }

    public long recordSegmentStart(String name, Map<String, String> metadata) {
        long timeStamp = mTimestampGenerator.generateTimeStamp();
        long metricId = mMetricId.getAndIncrement();
        post(() -> {
            MetricsEvent event = MetricsEvent.builder()
                    .metricId(metricId)
                    .metricName(name)
                    .eventType(MetricEventType.SEGMENT_START)
                    .metaData(metadata)
                    .relateId(0)
                    .value(timeStamp)
                    .build();
            recordMetric(event);
        });
        return metricId;
    }

    public void recordSegmentEnd(String name, Map<String, String> metadata, long segmentStartId) {
        long timeStamp = mTimestampGenerator.generateTimeStamp();
        post(() -> {
            long metricId = mMetricId.getAndIncrement();
            MetricsEvent event = MetricsEvent.builder()
                    .metricId(metricId)
                    .metricName(name)
                    .eventType(MetricEventType.SEGMENT_END)
                    .metaData(metadata)
                    .relateId(segmentStartId)
                    .value(timeStamp)
                    .build();
            recordMetric(event);
        });
    }

    public void recordSegmentFailed(String name, Map<String, String> metadata, long segmentStartId) {
        long timeStamp = mTimestampGenerator.generateTimeStamp();
        post(() -> {
            long metricId = mMetricId.getAndIncrement();
            MetricsEvent event = MetricsEvent.builder()
                    .metricId(metricId)
                    .metricName(name)
                    .eventType(MetricEventType.SEGMENT_FAILED)
                    .metaData(metadata)
                    .relateId(segmentStartId)
                    .value(timeStamp)
                    .build();
            recordMetric(event);
        });
    }

    public void recordCounter(String name, long amount) {
        post(() -> {
            long metricId = mMetricId.getAndIncrement();
            MetricsEvent event = MetricsEvent.builder()
                    .metricId(metricId)
                    .metricName(name)
                    .eventType(MetricEventType.COUNTER)
                    .metaData(cloneMetaData())
                    .relateId(0)
                    .value(amount)
                    .build();
            recordMetric(event);
        });
    }

     private void post(Runnable task) {
       mHandler.post(task);
    }
}
