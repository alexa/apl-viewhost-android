/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.FrameMetricsDomainCommandResponse;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class FrameMetricsEnableCommandRequest extends SessionCommandRequest<FrameMetricsDomainCommandResponse> {
    private static final String TAG = FrameMetricsEnableCommandRequest.class.getSimpleName();

    public FrameMetricsEnableCommandRequest(CommandRequestValidator commandRequestValidator,
                                            JSONObject obj,
                                            DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.FRAMEMETRICS_ENABLE, obj, commandRequestValidator, connection);
    }

    @Override
    public FrameMetricsDomainCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.FRAMEMETRICS_ENABLE+ " command");
        getSession().setFrameMetricsEventsEnabled(true);
        return new FrameMetricsDomainCommandResponse(getId(), getSessionId());
    }
}
