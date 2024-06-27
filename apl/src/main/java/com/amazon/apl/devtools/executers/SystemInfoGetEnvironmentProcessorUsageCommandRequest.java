/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.processor.SystemInfoGetEnvironmentProcessorUsageCommandRequestModel;
import com.amazon.apl.devtools.models.processor.SystemInfoGetEnvironmentProcessorUsageCommandResponse;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;
import com.amazon.apl.devtools.util.SystemCommandRunner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemInfoGetEnvironmentProcessorUsageCommandRequest extends SystemInfoGetEnvironmentProcessorUsageCommandRequestModel {
    public static final String TAG = SystemInfoGetEnvironmentProcessorUsageCommandRequest.class.getSimpleName();
    public SystemInfoGetEnvironmentProcessorUsageCommandRequest(JSONObject obj) throws JSONException {
        super(obj);
    }

    @Override
    public void execute(IDTCallback<SystemInfoGetEnvironmentProcessorUsageCommandResponse> callback) {
        try {
            Map<String, Integer> allProcessor = new HashMap<>();

            List<String> topCommandResults = SystemCommandRunner.executeTopCommand();
            // The line with the environment info looks like
            // 400%cpu   2%user   0%nice  13%sys 384%idle   0%iow   0%irq   0%sirq   0%host
            for (String line : topCommandResults) {
                String[] tokens = line.split("\\s+");
                if (tokens[0].contains("cpu")) {
                    for (String cpuMetric : tokens) {
                        String[] metric = cpuMetric.split("%");
                        allProcessor.put(metric[1], Integer.parseInt(metric[0]));
                    }
                }
            }

            if (allProcessor.containsKey("cpu") && allProcessor.containsKey("user") && allProcessor.containsKey("sys")) {
                float overallUser = allProcessor.get("user") * 100.f / allProcessor.get("cpu");
                float overallSystem = allProcessor.get("sys") * 100.f / allProcessor.get("cpu");
                callback.execute(new SystemInfoGetEnvironmentProcessorUsageCommandResponse(getId(), overallUser, overallSystem), RequestStatus.successful());
            } else {
                Log.e(TAG, "Unable to get usage statistics");
                callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to get processor information", e);
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
        }
    }
}
