/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.models.common.LogDomainResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.log.LogDisableCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import org.json.JSONException;
import org.json.JSONObject;

public class LogDisableCommandRequest extends LogDisableCommandRequestModel {
    private static final String TAG = LogDisableCommandRequest.class.getSimpleName();

    public LogDisableCommandRequest(CommandRequestValidator commandRequestValidator,
                                   JSONObject obj,
                                   DTConnection connection) throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public LogDomainResponse execute() {
        Log.i(TAG, "Executing " + getStringMethod() + " command");
        getSession().setLogEnabled(false);
        return new LogDomainResponse(getId(), getSessionId());
    }
}
