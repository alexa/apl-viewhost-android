/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.memory;

import android.os.Debug;
import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MemoryGetMemoryCommandResponse extends Response {
    private static final String TAG = MemoryGetMemoryCommandResponse.class.getSimpleName();

    private Debug.MemoryInfo mMemoryInfo;

    public MemoryGetMemoryCommandResponse(int id, Debug.MemoryInfo memoryInfo) {
        super(id);
        mMemoryInfo = memoryInfo;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + CommandMethod.MEMORY_GET_MEMORY + " response object");

        // For a standard total, we're calculating Resident Set Size (RSS), which includes both
        // private and shared, both clean and dirty.
        final int total = mMemoryInfo.getTotalPrivateClean() +
                mMemoryInfo.getTotalPrivateDirty() +
                mMemoryInfo.getTotalSharedClean() +
                mMemoryInfo.getTotalSharedDirty();

        // In addition, we'll add platform-specific memory stats
        Map<String, Integer> stats = new HashMap<String, Integer>();
        stats.put("dalvikPrivateDirty", mMemoryInfo.dalvikPrivateDirty);
        stats.put("dalvikPss", mMemoryInfo.dalvikPss);
        stats.put("dalvikSharedDirty", mMemoryInfo.dalvikSharedDirty);
        stats.put("nativePrivateDirty", mMemoryInfo.nativePrivateDirty);
        stats.put("nativePss", mMemoryInfo.nativePss);
        stats.put("nativeSharedDirty", mMemoryInfo.nativeSharedDirty);
        stats.put("otherPrivateDirty", mMemoryInfo.otherPrivateDirty);
        stats.put("otherPss", mMemoryInfo.otherPss);
        stats.put("otherSharedDirty", mMemoryInfo.otherSharedDirty);

        JSONArray statsArray = new JSONArray();
        for (Map.Entry<String, Integer> stat : stats.entrySet()) {
            statsArray.put(new JSONObject().put("name", stat.getKey()).put("value", stat.getValue()));
        }

        JSONObject result = new JSONObject();
        result.put("total", total);
        result.put("stats", statsArray);
        return super.toJSONObject().put("result", result);
    }
}
