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

import java.io.IOException;
import java.util.List;

public class SystemInfoGetEnvironmentProcessorUsageCommandRequestApi25 extends SystemInfoGetEnvironmentProcessorUsageCommandRequestModel {
    public static final String TAG = SystemInfoGetEnvironmentProcessorUsageCommandRequestApi25.class.getSimpleName();
    public SystemInfoGetEnvironmentProcessorUsageCommandRequestApi25(JSONObject obj) throws JSONException {
        super(obj);
    }

    /**
     * Executes the top command to get a list of running processes, and parses the response to get the
     * usage percentage for the environment.
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
    public void execute(IDTCallback<SystemInfoGetEnvironmentProcessorUsageCommandResponse> callback) {
        List<String> topCommandResults = null;
        try {
            topCommandResults = SystemCommandRunner.executeCommand("top", "-n", "1");
        } catch (IOException e) {
            Log.e(TAG, "Unable to get processor information", e);
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
            return;
        }

        if (topCommandResults.isEmpty()) {
            Log.e(TAG, "Command execution was empty");
            callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
            return;
        }
        // User 0%, System 0%, IOW 0%, IRQ 0%
        String[] tokens = topCommandResults.get(0).trim().split("\\s+");
        String userPercent = tokens[1].substring(0, tokens[1].length()-2);
        String systemPercent = tokens[3].substring(0, tokens[3].length()-2);
        float overallUser = Float.parseFloat(userPercent);
        float overallSystem = Float.parseFloat(systemPercent);
        callback.execute(new SystemInfoGetEnvironmentProcessorUsageCommandResponse(getId(), overallUser, overallSystem), RequestStatus.successful());
    }
}
