/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics.impl;

import com.amazon.apl.android.metrics.ITimer;

import java.util.HashMap;
import java.util.Map;

/**
 * Timers emit a StartSegment event upon creation and a EndSegment event upon either stop or fail.
 */
public class Timer implements ITimer {
    private static final String SUCCESS_KEY = "Succeeded";
    private String mName;
    private Map<String, String> mMetadata;
    private long mSegmentStartId;

    private MetricsRecorder mMetricsRecorder;

    Timer(final String name, final Map<String, String> metadata, final MetricsRecorder metricsRecorder) {
        mName = name;
        mMetadata = metadata == null ? new HashMap<>() : metadata;
        mMetricsRecorder = metricsRecorder;
        mSegmentStartId = metricsRecorder.recordSegmentStart(name, cloneMetaData());
    }

    @Override
    public void stop() {
        mMetricsRecorder.recordSegmentEnd(mName, mMetadata, mSegmentStartId);
    }

    @Override
    public void fail() {
        mMetricsRecorder.recordSegmentFailed(mName, mMetadata, mSegmentStartId);
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
}
