/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.performance;

import android.util.Log;

import com.amazon.apl.android.utils.MetricInfo;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.PerformanceDomainCommandResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PerformanceGetMetricsCommandResponse extends PerformanceDomainCommandResponse {
    private static final String TAG = PerformanceGetMetricsCommandResponse.class.getSimpleName();
    private final List<MetricInfo> mPerformanceMetrics;

    public PerformanceGetMetricsCommandResponse(int id, String sessionId, List<MetricInfo> performanceMetrics) {
        super(id, sessionId);
        mPerformanceMetrics = performanceMetrics;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + CommandMethod.PERFORMANCE_GET_METRICS + " response object");
        // Parse collection of MetricInfo into a JSONArray
        JSONArray metricInfoJSONArray = new JSONArray();
        for (MetricInfo metricInfo : mPerformanceMetrics) {
            metricInfoJSONArray.put(new JSONObject().put("name", metricInfo.getName())
                    .put("value", metricInfo.getValue()));
        }
        return super.toJSONObject().put("result", new JSONObject()
                .put("metrics", metricInfoJSONArray));
    }

}




