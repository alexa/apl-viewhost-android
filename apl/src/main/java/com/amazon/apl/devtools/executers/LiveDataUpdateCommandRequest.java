/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.livedata.LiveDataUpdateCommandRequestModel;
import com.amazon.apl.devtools.models.livedata.LiveDataUpdateCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import com.amazon.apl.devtools.util.IDTCallback;
import org.json.JSONException;
import org.json.JSONObject;

public final class LiveDataUpdateCommandRequest
        extends LiveDataUpdateCommandRequestModel implements ICommandValidator {
    private static final String TAG = LiveDataUpdateCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private ViewTypeTarget mViewTypeTarget;

    public LiveDataUpdateCommandRequest(CommandRequestValidator commandRequestValidator,
                                        JSONObject obj,
                                        DTConnection connection)
            throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public void execute(IDTCallback<LiveDataUpdateCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.LIVE_DATA_UPDATE + " command");
        mViewTypeTarget.updateLiveData(getParams().getName(), getParams().getOperations(), (result, requestStatus) ->
            callback.execute(new LiveDataUpdateCommandResponse(getId(), getSessionId(), result), requestStatus));
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.LIVE_DATA_UPDATE + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        Session session = mConnection.getSession(getSessionId());
        // TODO:: Validate target type before casting when more target types are added
        mViewTypeTarget = (ViewTypeTarget) session.getTarget();
    }
}
