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
import com.amazon.apl.devtools.models.input.InputTouchCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.IDTCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class InputTouchCommandRequest extends InputTouchCommandRequestModel implements ICommandValidator {
    private static final String TAG = "InputTouchCmdRequest";
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private Session mSession;
    private ViewTypeTarget mViewTypeTarget;
    public InputTouchCommandRequest(CommandMethod method,
                                    CommandRequestValidator commandRequestValidator,
                                    JSONObject obj,
                                    DTConnection connection) throws JSONException, DTException {
        super(method, obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public void execute(IDTCallback<InputDomainCommandResponse> callback) {
        Params params = getParams();
        mViewTypeTarget.enqueueInputEvents(getId(), params.getEvents(mViewTypeTarget.getDisplayRefreshRate()), (result, requestStatus) ->
                callback.execute(new InputDomainCommandResponse(getId(), getSessionId(), result), requestStatus));
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.INPUT_TOUCH + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        mSession = mConnection.getSession(getSessionId());
        mViewTypeTarget = (ViewTypeTarget) mSession.getTarget();
    }
}
