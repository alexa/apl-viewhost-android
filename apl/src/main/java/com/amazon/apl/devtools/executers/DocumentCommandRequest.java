/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.common.DocumentDomainResponse;
import com.amazon.apl.devtools.models.document.DocumentCommandRequestModel;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;
import org.json.JSONException;
import org.json.JSONObject;

public class DocumentCommandRequest extends DocumentCommandRequestModel {
    private static final String TAG = DocumentCommandRequest.class.getSimpleName();

    public DocumentCommandRequest(CommandMethod commandMethod,
                                                CommandRequestValidator commandRequestValidator,
                                                JSONObject obj,
                                                DTConnection connection) throws JSONException, DTException {
        super(commandMethod, obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback<DocumentDomainResponse> callback) {
        Log.i(TAG, "Executing " + getStringMethod() + " command");
        getViewTypeTarget().documentCommandRequest(getId(), getStringMethod(), getParams(), (result, status) -> {
            if (status.getExecutionStatus() == RequestStatus.ExecutionStatus.SUCCESSFUL) {
                try {
                    if (result == null) {
                        callback.execute(new DocumentDomainResponse(getId(), getSessionId()), status);
                    } else {
                        callback.execute(new DocumentDomainResponse(getId(), getSessionId(), new JSONObject(result)), status);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating json object for " + getStringMethod() + " for result: " + result);
                    callback.execute(RequestStatus.failed(getId(), DTError.METHOD_FAILURE));
                }
            }
        });
    }
}
