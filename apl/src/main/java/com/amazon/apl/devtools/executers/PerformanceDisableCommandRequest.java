/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.common.PerformanceDomainCommandResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.performance.PerformanceDisableCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceDisableCommandRequest extends PerformanceDisableCommandRequestModel {
    private static final String TAG = PerformanceDisableCommandRequest.class.getSimpleName();

    public PerformanceDisableCommandRequest(CommandRequestValidator commandRequestValidator,
                                            JSONObject obj,
                                            DTConnection connection) throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public PerformanceDomainCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.PERFORMANCE_DISABLE + " command");
        // Disable the performance metric for this session
        getSession().setPerformanceEnabled(false);
        return new PerformanceDomainCommandResponse(getId(), getSessionId());
    }
}
