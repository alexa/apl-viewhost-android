/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for Command Requests that require a DeveloperTools {@link Session}
 * @param <TResponse>
 */
public abstract class SessionCommandRequest<TResponse extends Response> extends Request<TResponse> {
    private final DTConnection mConnection;

    private final CommandMethod mMethod;

    private final String mSessionId;

    private final ViewTypeTarget mViewTypeTarget;

    private final Session mSession;

    protected DTConnection getConnection() {
        return mConnection;
    }

    protected Session getSession() {
        return mSession;
    }

    protected String getSessionId() {
        return mSessionId;
    }

    protected String getStringMethod() {
        return mMethod.toString();
    }

    protected ViewTypeTarget getViewTypeTarget() {
        return mViewTypeTarget;
    }

    protected SessionCommandRequest(CommandMethod method,
                                    JSONObject obj,
                                    CommandRequestValidator commandRequestValidator,
                                    DTConnection connection) throws JSONException, DTException {
        super(method, obj);
        mConnection = connection;
        mMethod = method;
        mSessionId = obj.getString("sessionId");
        Log.i(getClass().getSimpleName(), "Validating " + method.toString() + " command");
        commandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), connection);
        mSession = connection.getSession(getSessionId());
        mViewTypeTarget = (ViewTypeTarget) mSession.getTarget();
        // Any extra validation to be done by individual command requests
        validate();
    }
}
