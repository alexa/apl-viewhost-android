/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.processor.SystemInfoGetApplicationProcessorUsageCommandRequestModel;
import com.amazon.apl.devtools.models.processor.SystemInfoGetApplicationProcessorUsageCommandResponse;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;
import com.amazon.apl.devtools.util.SystemCommandRunner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class SystemInfoGetApplicationProcessorUsageCommandRequest extends SystemInfoGetApplicationProcessorUsageCommandRequestModel {
    private static final String TAG = SystemInfoGetApplicationProcessorUsageCommandRequest.class.getSimpleName();

    public SystemInfoGetApplicationProcessorUsageCommandRequest(JSONObject obj) throws JSONException {
        super(obj);
    }

    @Override
    public void execute(IDTCallback<SystemInfoGetApplicationProcessorUsageCommandResponse> callback) {
        try {
            int maxCpuPercentage = -1;
            float usagePercentage = -1.f;

            List<String> topCommandResults = SystemCommandRunner.executeTopCommand();
            for (String line : topCommandResults) {
                String[] tokens = line.split("\\s+");
                if (tokens[0].contains("cpu")) {
                    String[] metric = tokens[0].split("%");
                    maxCpuPercentage = Integer.parseInt(metric[0]);
                } else if (tokens.length == 2 && Integer.parseInt(tokens[0]) == android.os.Process.myPid()) {
                    usagePercentage = Float.parseFloat(tokens[1]) * 100 / maxCpuPercentage;
                }
            }

            if (maxCpuPercentage >= 0 && usagePercentage >= 0) {
                callback.execute(new SystemInfoGetApplicationProcessorUsageCommandResponse(getId(), usagePercentage), RequestStatus.successful());
            } else {
                callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to get processor information", e);
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
        }
    }
}
