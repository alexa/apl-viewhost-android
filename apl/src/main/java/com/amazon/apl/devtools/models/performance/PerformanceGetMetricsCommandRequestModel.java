/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.performance;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.PerformanceDomainCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;


public abstract class PerformanceGetMetricsCommandRequestModel
        extends PerformanceDomainCommandRequest<PerformanceGetMetricsCommandResponse> {

    protected PerformanceGetMetricsCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.PERFORMANCE_GET_METRICS, obj);
    }
}