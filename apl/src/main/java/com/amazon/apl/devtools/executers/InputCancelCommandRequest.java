/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.common.InputDomainCommandResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.input.InputCancelCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;

import org.json.JSONException;
import org.json.JSONObject;

public class InputCancelCommandRequest extends InputCancelCommandRequestModel implements ICommandValidator {
    private static final String TAG = "InputCancelCmdRequest";
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private Session mSession;
    private ViewTypeTarget mViewTypeTarget;
    public InputCancelCommandRequest(CommandMethod method,
                                     CommandRequestValidator commandRequestValidator,
                                     JSONObject obj,
                                     DTConnection connection) throws JSONException, DTException {
        super(method, obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public InputDomainCommandResponse execute() {
        mViewTypeTarget.clearInputEvents();
        return new InputDomainCommandResponse(getId(), getSessionId());
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.INPUT_CANCEL + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        mSession = mConnection.getSession(getSessionId());
        mViewTypeTarget = (ViewTypeTarget) mSession.getTarget();
    }
}
