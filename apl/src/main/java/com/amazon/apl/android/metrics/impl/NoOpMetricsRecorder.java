package com.amazon.apl.android.metrics.impl;

import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.ITimer;

import java.util.HashMap;
import java.util.Map;

/**
 * The NoOpMetricsRecorder is a NoOp implementation of a MetricsRecorder
 */
public class NoOpMetricsRecorder extends APLMetricsRecorder {
    @Override
    public void mergeMetadata(String key, String value) {

    }

    @Override
    public void mergeMetadata(Map<String, String> metadata) {

    }

    @Override
    public ICounter createCounter(String counterName) {
        return new Counter(counterName, this);
    }

    @Override
    public void recordMilestone(String milestoneName) {

    }

    @Override
    public ITimer startTimer(String timerName, Map<String, String> metadata) {
        return new Timer(timerName, new HashMap<>(), this);
    }

    @Override
    public void addSink(IMetricsSink sink) {

    }

    @Override
    public long recordSegmentStart(String name, Map<String, String> metadata) {
        return 0;
    }

    @Override
    public void recordSegmentEnd(String name, Map<String, String> metadata, long segmentStartId) {

    }

    @Override
    public void recordSegmentFailed(String name, Map<String, String> metadata, long segmentStartId) {

    }

    @Override
    public void recordCounter(String name, long amount) {

    }
}
