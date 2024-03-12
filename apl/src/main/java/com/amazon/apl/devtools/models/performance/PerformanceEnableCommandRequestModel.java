/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.performance;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.PerformanceDomainCommandRequest;
import com.amazon.apl.devtools.models.common.PerformanceDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceEnableCommandRequestModel extends PerformanceDomainCommandRequest<PerformanceDomainCommandResponse> {

    protected PerformanceEnableCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.PERFORMANCE_ENABLE, obj);
    }
}
