/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.Target;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.target.TargetAttachToTargetCommandRequestModel;
import com.amazon.apl.devtools.models.target.TargetAttachToTargetCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.TargetCatalog;

import org.json.JSONException;
import org.json.JSONObject;

public final class TargetAttachToTargetCommandRequest
        extends TargetAttachToTargetCommandRequestModel implements ICommandValidator {
    private static final String TAG = TargetAttachToTargetCommandRequest.class.getSimpleName();
    private final TargetCatalog mTargetCatalog;
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private Target mTarget;

    public TargetAttachToTargetCommandRequest(TargetCatalog targetCatalog,
                                              CommandRequestValidator commandRequestValidator,
                                              JSONObject obj,
                                              DTConnection connection)
            throws JSONException, DTException {
        super(obj);
        mTargetCatalog = targetCatalog;
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    private String getTargetId() {
        return getParams().getTargetId();
    }

    @Override
    public TargetAttachToTargetCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.TARGET_ATTACH_TO_TARGET + " command");
        Session session = new Session(mConnection, mTarget);
        return new TargetAttachToTargetCommandResponse(getId(), session.getSessionId());
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.TARGET_ATTACH_TO_TARGET + " command");
        mCommandRequestValidator.validateBeforeGettingTargetFromTargetCatalog(getId(),
                getTargetId());
        mTarget = mTargetCatalog.get(getTargetId());
        mCommandRequestValidator.validateBeforeCreatingSession(getId(), mConnection, mTarget);
    }
}
