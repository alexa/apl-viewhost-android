/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.enums;

import androidx.annotation.NonNull;

public enum EventMethod {
    VIEW_STATE_CHANGE(EventMethod.VIEW_STATE_CHANGE_TEXT),
    LOG_ENTRY_ADDED(EventMethod.LOG_ENTRY_ADDED_TEXT),
    NETWORK_REQUEST_WILL_BE_SENT(EventMethod.NETWORK_REQUEST_WILL_BE_SENT_TEXT),
    NETWORK_LOADING_FAILED(EventMethod.NETWORK_LOADING_FAILED_TEXT),
    NETWORK_LOADING_FINISHED(EventMethod.NETWORK_LOADING_FINISHED_TEXT),
    PERFORMANCE_METRIC(EventMethod.PERFORMANCE_METRIC_TEXT),
    FRAMEMETRICS_INCIDENT_REPORTED(EventMethod.FRAMEMETRICS_INCIDENT_REPORTED_TEXT);

    private static final String VIEW_STATE_CHANGE_TEXT = "View.stateChange";
    private static final String LOG_ENTRY_ADDED_TEXT = "Log.entryAdded";
    private static final String NETWORK_REQUEST_WILL_BE_SENT_TEXT = "Network.requestWillBeSent";
    private static final String NETWORK_LOADING_FAILED_TEXT = "Network.loadingFailed";
    private static final String NETWORK_LOADING_FINISHED_TEXT = "Network.loadingFinished";
    private static final String PERFORMANCE_METRIC_TEXT = "Performance.metrics";
    private static final String FRAMEMETRICS_INCIDENT_REPORTED_TEXT = "FrameMetrics.incidentReported";
    private final String mEventMethodText;

    EventMethod(String eventMethodText) {
        mEventMethodText = eventMethodText;
    }

    @NonNull
    @Override
    public String toString() {
        return mEventMethodText;
    }
}
