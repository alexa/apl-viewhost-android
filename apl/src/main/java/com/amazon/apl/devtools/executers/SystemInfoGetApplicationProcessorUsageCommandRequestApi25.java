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

public class SystemInfoGetApplicationProcessorUsageCommandRequestApi25 extends SystemInfoGetApplicationProcessorUsageCommandRequestModel {
    private static final String TAG = SystemInfoGetApplicationProcessorUsageCommandRequestApi25.class.getSimpleName();

    public SystemInfoGetApplicationProcessorUsageCommandRequestApi25(JSONObject obj) throws JSONException {
        super(obj);
    }

    /**
     * Executes the top command to get a list of running processes, and parses the response to get the
     * usage percentage for the current process.
     *
     * Expected format of the top command output looks like,
     * User 0%, System 25%, IOW 0%, IRQ 0%
     * User 0 + Nice 0 + Sys 1 + Idle 3 + IOW 0 + IRQ 0 + SIRQ 0 = 4
     *   PID USER     PR  NI CPU% S  #THR     VSS     RSS PCY Name
     *  1424 shell    20   0  25% R     1   6268K   1668K  fg top
     *  with multiple processes listed.
     *
     * @param callback The DevTools callback to return the response.
     */
    @Override
    public void execute(IDTCallback<SystemInfoGetApplicationProcessorUsageCommandResponse> callback) {
        List<String> topCommandResults;

        try {
            topCommandResults = SystemCommandRunner.executeCommand("top", "-n", "1");
        } catch (IOException e) {
            Log.e(TAG, "Command execution failed.");
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
            return;
        }

        float usagePercentage = -1.f;
        int cpuIndex = -1;

        for (String line : topCommandResults) {
            if (line.contains("CPU")) {
                cpuIndex = findCpuIndex(line);
                continue;
            }
            String[] tokens = line.trim().split("\\s+");

            if (tokens[0].matches("[0-9]+") && Integer.parseInt(tokens[0]) == android.os.Process.myPid()) {
                String cpuPercent = tokens[cpuIndex];
                usagePercentage = Float.parseFloat(cpuPercent.substring(0, cpuPercent.length()-1));
                break;
            }
        }

        if (usagePercentage >= 0) {
            callback.execute(new SystemInfoGetApplicationProcessorUsageCommandResponse(getId(), usagePercentage), RequestStatus.successful());
        } else {
            Log.e(TAG, "Command results are empty.");
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
        }
    }

    private int findCpuIndex(String line) {
        String[] header = line.trim().split("\\s+");
        for (int i=0; i < header.length; i++) {
            if (header[i].contains("CPU")) {
                return i;
            }
        }
        return -1;
    }
}