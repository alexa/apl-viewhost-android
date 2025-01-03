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

import java.io.IOException;
import java.util.List;

public class SystemInfoGetApplicationProcessorUsageCommandRequest extends SystemInfoGetApplicationProcessorUsageCommandRequestModel {
    private static final String TAG = SystemInfoGetApplicationProcessorUsageCommandRequest.class.getSimpleName();

    public SystemInfoGetApplicationProcessorUsageCommandRequest(JSONObject obj) throws JSONException {
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
     * with this specific process listed.
     * @param callback The DevTools callback to return the response.
     */
    @Override
    public void execute(IDTCallback<SystemInfoGetApplicationProcessorUsageCommandResponse> callback) {
        List<String> topCommandResults;

        try {
            String appId = "-p" + android.os.Process.myPid();
            topCommandResults = SystemCommandRunner.executeCommand("top", appId, "-oPID,%CPU", "-b", "-n1");
        } catch (IOException e) {
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
            return;
        }

        int maxCpuPercentage = 100;
        float usagePercentage = -1.f;

        for (String line : topCommandResults) {
            String[] tokens = line.trim().split("\\s+");

            if (tokens[0].contains("cpu")) {
                String[] metric = tokens[0].split("%");
                maxCpuPercentage = Integer.parseInt(metric[0]);
            } else if (tokens.length == 2 && tokens[0].matches("[0-9]+") && Integer.parseInt(tokens[0]) == android.os.Process.myPid()) {
                usagePercentage = Float.parseFloat(tokens[1]) * 100 / maxCpuPercentage;
            }
        }

        if (usagePercentage >= 0) {
            callback.execute(new SystemInfoGetApplicationProcessorUsageCommandResponse(getId(), usagePercentage), RequestStatus.successful());
        } else {
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
        }
    }

}