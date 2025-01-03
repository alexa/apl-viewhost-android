/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
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
        extends LiveDataUpdateCommandRequestModel {
    private static final String TAG = LiveDataUpdateCommandRequest.class.getSimpleName();

    public LiveDataUpdateCommandRequest(CommandRequestValidator commandRequestValidator,
                                        JSONObject obj,
                                        DTConnection connection)
            throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback<LiveDataUpdateCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.LIVE_DATA_UPDATE + " command");
        getViewTypeTarget().updateLiveData(getParams().getName(), getParams().getOperations(), (result, requestStatus) ->
            callback.execute(new LiveDataUpdateCommandResponse(getId(), getSessionId(), result), requestStatus));
    }
}
