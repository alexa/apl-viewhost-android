/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.memory;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.Request;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class MemoryGetMemoryCommandRequestModel extends Request<MemoryGetMemoryCommandResponse> {
    protected MemoryGetMemoryCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.MEMORY_GET_MEMORY, obj);
    }
}
