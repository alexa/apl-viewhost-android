/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.processor;

import com.amazon.apl.devtools.models.common.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemInfoGetApplicationProcessorUsageCommandResponse extends Response {
    private final float applicationProcessor;
    public SystemInfoGetApplicationProcessorUsageCommandResponse(int id, float applicationProcessor) {
        super(id);
        this.applicationProcessor = applicationProcessor;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {

        JSONObject result = new JSONObject();
        result.put("usage", applicationProcessor);

        return super.toJSONObject().put("result", result);
    }
}
