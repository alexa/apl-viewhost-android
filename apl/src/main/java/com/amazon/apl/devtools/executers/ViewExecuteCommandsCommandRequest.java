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
import com.amazon.apl.devtools.models.view.ViewExecuteCommandsCommandRequestModel;
import com.amazon.apl.devtools.models.view.ViewExecuteCommandsCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import com.amazon.apl.devtools.util.IDTCallback;
import org.json.JSONException;
import org.json.JSONObject;

public final class ViewExecuteCommandsCommandRequest
        extends ViewExecuteCommandsCommandRequestModel {
    private static final String TAG = ViewExecuteCommandsCommandRequest.class.getSimpleName();

    public ViewExecuteCommandsCommandRequest(CommandRequestValidator commandRequestValidator,
                                             JSONObject obj,
                                             DTConnection connection)
            throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback<ViewExecuteCommandsCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.VIEW_EXECUTE_COMMANDS + " command");

        /*
         * Passing a callback to consume the execute commands status in order to create a response.
         * The response will be consumed by the caller of this execute method.
         */
        getViewTypeTarget().executeCommands(getParams().getCommands(), (status, requestStatus) ->
            callback.execute(new ViewExecuteCommandsCommandResponse(getId(), getSessionId(), status), requestStatus));
    }
}
