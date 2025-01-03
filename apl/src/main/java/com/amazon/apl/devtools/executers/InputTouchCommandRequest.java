/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.InputDomainCommandResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.input.InputTouchCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.IDTCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class InputTouchCommandRequest extends InputTouchCommandRequestModel {
    public InputTouchCommandRequest(CommandMethod method,
                                    CommandRequestValidator commandRequestValidator,
                                    JSONObject obj,
                                    DTConnection connection) throws JSONException, DTException {
        super(method, obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback callback) {
        Params params = getParams();
        getViewTypeTarget().enqueueInputEvents(getId(), params.getEvents(getViewTypeTarget().getDisplayRefreshRate()), (result, requestStatus) ->
                callback.execute(new InputDomainCommandResponse(getId(), getSessionId(), result), requestStatus));
    }
}
