/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;
import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.target.DetachFromTargetCommandResponse;
import com.amazon.apl.devtools.models.target.DetachFromTargetModelRequest;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.DependencyContainer;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;
import com.amazon.apl.devtools.util.TargetCatalog;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to handle Target.detachFromTarget request to terminate the session
 */
public class DetachFromTargetCommandRequest extends DetachFromTargetModelRequest implements ICommandValidator {
    private static final String TAG = DetachFromTargetCommandRequest.class.getSimpleName();
    private final DTConnection mConnection;
    private final CommandRequestValidator mCommandRequestValidator;

    public DetachFromTargetCommandRequest(CommandRequestValidator commandRequestValidator,
                                          JSONObject obj, DTConnection connection) throws DTException, JSONException {
        super(obj);
        mConnection = connection;
        mCommandRequestValidator = commandRequestValidator;
        validate();
    }

    @Override
    public void execute(IDTCallback<DetachFromTargetCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.TARGET_DETACH_FROM_TARGET + " command");
        TargetCatalog targetCatalog = DependencyContainer.getInstance().getTargetCatalog();
        targetCatalog.post(() -> {
            Session session = mConnection.getSession(getParams().getSessionId());
            session.destroy();
            callback.execute(new DetachFromTargetCommandResponse(getId()), RequestStatus.successful());
        });
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.TARGET_DETACH_FROM_TARGET + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getParams().getSessionId(), mConnection);
    }
}
