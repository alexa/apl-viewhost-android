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
import com.amazon.apl.devtools.models.log.LogEnableCommandRequestModel;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import org.json.JSONException;
import org.json.JSONObject;

public class LogEnableCommandRequest extends LogEnableCommandRequestModel implements ICommandValidator{
    private static final String TAG = LogEnableCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;

    public LogEnableCommandRequest(CommandRequestValidator commandRequestValidator,
                                   JSONObject obj,
                                   DTConnection connection) throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public LogDomainResponse execute() {
        mConnection.getSession(getSessionId()).setLogEnabled(true);
        return new LogDomainResponse(getId(), getSessionId());
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.LOG_ENABLE + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
    }
}
