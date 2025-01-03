/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.InputDomainCommandResponse;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class InputCancelCommandRequest extends SessionCommandRequest<InputDomainCommandResponse> {
    public InputCancelCommandRequest(CommandMethod method,
                                     CommandRequestValidator commandRequestValidator,
                                     JSONObject obj,
                                     DTConnection connection) throws JSONException, DTException {
        super(method, obj, commandRequestValidator, connection);
    }

    @Override
    public InputDomainCommandResponse execute() {
        getViewTypeTarget().clearInputEvents();
        return new InputDomainCommandResponse(getId(), getSessionId());
    }
}
