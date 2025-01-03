/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ViewExecuteCommandsCommandRequestModel
        extends SessionCommandRequest<ViewExecuteCommandsCommandResponse> {
    private final Params mParams;

    protected ViewExecuteCommandsCommandRequestModel(JSONObject obj, CommandRequestValidator commandRequestValidator, DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.VIEW_EXECUTE_COMMANDS, obj, commandRequestValidator, connection);
        try {
            mParams = new Params(obj.getJSONObject("params").getJSONArray("commands")
                    .toString());
        } catch (JSONException e) {
            throw new DTException(getId(), DTError.INVALID_DOCUMENT.getErrorCode(),
                    "Invalid commands document", e);
        }
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final String mCommands;

        private Params(String commands) {
            mCommands = commands;
        }

        public String getCommands() {
            return mCommands;
        }
    }
}
