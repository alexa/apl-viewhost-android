/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.os.Debug;
import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.memory.MemoryGetMemoryCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class MemoryGetMemoryCommandRequest extends SessionCommandRequest<MemoryGetMemoryCommandResponse> {
    private static final String TAG = MemoryGetMemoryCommandRequest.class.getSimpleName();

    public MemoryGetMemoryCommandRequest(CommandRequestValidator commandRequestValidator, JSONObject obj, DTConnection connection)
            throws JSONException, DTException {
        super(CommandMethod.MEMORY_GET_MEMORY, obj, commandRequestValidator, connection);
    }

    @Override
    public MemoryGetMemoryCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.MEMORY_GET_MEMORY + " command");

        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);

        return new MemoryGetMemoryCommandResponse(getId(), memoryInfo);
    }
}
