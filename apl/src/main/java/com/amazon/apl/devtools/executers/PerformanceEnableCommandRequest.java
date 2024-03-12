/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.common.PerformanceDomainCommandResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.performance.PerformanceEnableCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class PerformanceEnableCommandRequest extends PerformanceEnableCommandRequestModel implements ICommandValidator {
    private static final String TAG = PerformanceEnableCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private Session mSession;

    public PerformanceEnableCommandRequest(CommandRequestValidator commandRequestValidator,
                                           JSONObject obj,
                                           DTConnection connection) throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public PerformanceDomainCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.PERFORMANCE_ENABLE + " command");
        // Enable the performance metric for this session
        mSession.setPerformanceEnabled(true);
        return new PerformanceDomainCommandResponse(getId(), getSessionId());
    }


    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.PERFORMANCE_ENABLE + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        mSession = mConnection.getSession(getSessionId());
    }
}
