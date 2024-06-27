/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.processor;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.Request;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemInfoGetEnvironmentProcessorUsageCommandRequestModel extends Request<SystemInfoGetEnvironmentProcessorUsageCommandResponse> {
    protected SystemInfoGetEnvironmentProcessorUsageCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.SYSTEM_INFO_GET_ENVIRONMENT_PROCESSOR_USAGE, obj);
    }
}
