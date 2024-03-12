/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.performance;

import com.amazon.apl.android.utils.MetricInfo;
import com.amazon.apl.devtools.enums.EventMethod;
import com.amazon.apl.devtools.models.common.Event;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceMetricsEvent extends Event {
    private final List<MetricInfo> mPerformanceMetrics;
    public PerformanceMetricsEvent(String mSessionId, List<MetricInfo> performanceMetrics) {
        super(EventMethod.PERFORMANCE_METRIC, mSessionId);
        mPerformanceMetrics = performanceMetrics;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONArray metricInfoJSONArray = new JSONArray();
        for (MetricInfo metricInfo : mPerformanceMetrics) {
            metricInfoJSONArray.put(new JSONObject().put("name", metricInfo.getName())
                    .put("value", metricInfo.getValue()));
        }
        return super.toJSONObject().put("params", new JSONObject().put("metrics", metricInfoJSONArray));
    }
}
