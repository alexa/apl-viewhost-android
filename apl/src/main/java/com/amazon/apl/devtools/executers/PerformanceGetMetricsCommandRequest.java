/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.performance.PerformanceGetMetricsCommandRequestModel;
import com.amazon.apl.devtools.models.performance.PerformanceGetMetricsCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import com.amazon.apl.devtools.util.IDTCallback;
import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceGetMetricsCommandRequest extends PerformanceGetMetricsCommandRequestModel {
    private static final String TAG = PerformanceGetMetricsCommandRequest.class.getSimpleName();

    public PerformanceGetMetricsCommandRequest(CommandRequestValidator commandRequestValidator,
                                               JSONObject obj,
                                               DTConnection connection)
            throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback<PerformanceGetMetricsCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.PERFORMANCE_GET_METRICS + " command");
        getViewTypeTarget().getPerformanceMetrics(getId(), (performanceMetrics, requestStatus) ->
                callback.execute(new PerformanceGetMetricsCommandResponse(
                        getId(), getSessionId(), performanceMetrics), requestStatus));
    }


    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.PERFORMANCE_GET_METRICS + " command");
        if (!getSession().isPerformanceEnabled()) {
            throw new DTException(getId(), DTError.PERFORMANCE_ALREADY_DISABLED.getErrorCode(),
                    "Performance metrics not enabled");
        }
    }
}