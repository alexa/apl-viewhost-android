/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.os.Build;
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

    /**
     * Executes the top command to get a list of running processes, and parses the response to get the
     * usage percentage for the current process.
     * Tasks: 174 total,   1 running, 173 sleeping,   0 stopped,   0 zombie
     *   Mem:      1.9G total,      1.6G used,      291M free,       17M buffers
     *  Swap:      1.4G total,      256K used,      1.4G free,      925M cached
     * 400%cpu 126%user  44%nice 238%sys   9%idle   0%iow   0%irq   0%sirq   0%host
     *   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
     *  1976 system       18  -2 2.0G 269M 216M D 58.3  13.4   0:10.52 system_server
     * with multiple processes listed.
     * @param callback The DevTools callback to return the response.
     */
    @Override
    public void execute(IDTCallback<SystemInfoGetEnvironmentProcessorUsageCommandResponse> callback) {
        try {
            Map<String, Integer> allProcessor = new HashMap<>();
            float overallUser, overallSystem;

            String appId = "-p" + android.os.Process.myPid();
            List<String> topCommandResults = SystemCommandRunner.executeCommand("top", appId, "-oPID,%CPU", "-b", "-n1");
            for (String line : topCommandResults) {
                String[] tokens = line.split("\\s+");
                if (tokens[0].contains("cpu")) {
                    for (String cpuMetric : tokens) {
                        String[] metric = cpuMetric.split("%");
                        allProcessor.put(metric[1], Integer.parseInt(metric[0]));
                    }
                }
            }

            if (!allProcessor.containsKey("cpu") || !allProcessor.containsKey("user") || !allProcessor.containsKey("sys")) {
                Log.e(TAG, "Unable to get usage statistics");
                callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
                return;
            }
            overallUser = allProcessor.get("user") * 100.f / allProcessor.get("cpu");
            overallSystem = allProcessor.get("sys") * 100.f / allProcessor.get("cpu");

            callback.execute(new SystemInfoGetEnvironmentProcessorUsageCommandResponse(getId(), overallUser, overallSystem), RequestStatus.successful());
        } catch (IOException e) {
            Log.e(TAG, "Unable to get processor information", e);
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
        }
    }
}
