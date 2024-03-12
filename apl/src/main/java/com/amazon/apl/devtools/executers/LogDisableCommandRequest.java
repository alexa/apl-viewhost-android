/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;
import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.common.LogDomainResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.log.LogDisableCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import org.json.JSONException;
import org.json.JSONObject;

public class LogDisableCommandRequest extends LogDisableCommandRequestModel implements ICommandValidator {
    private static final String TAG = LogDisableCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private Session mSession;

    public LogDisableCommandRequest(CommandRequestValidator commandRequestValidator,
                                   JSONObject obj,
                                   DTConnection connection) throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public LogDomainResponse execute() {
        mSession.setLogEnabled(false);
        return new LogDomainResponse(getId(), getSessionId());
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.LOG_DISABLE + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        mSession = mConnection.getSession(getSessionId());
        mCommandRequestValidator.validateLogEnabled(getId(), getSessionId(), mSession.isLogEnabled());
    }
}
