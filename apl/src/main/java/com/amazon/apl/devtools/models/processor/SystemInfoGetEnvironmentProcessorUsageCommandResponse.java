/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.processor;

import com.amazon.apl.devtools.models.common.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemInfoGetEnvironmentProcessorUsageCommandResponse extends Response {
    final float overallUser;
    final float overallSystem;

    public SystemInfoGetEnvironmentProcessorUsageCommandResponse(int id, float overallUser, float overallSystem) {
        super(id);
        this.overallUser = overallUser;
        this.overallSystem = overallSystem;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject result = new JSONObject();

        result.put("system", overallSystem);
        result.put("user", overallUser);

        return super.toJSONObject().put("result", result);
    }
}
