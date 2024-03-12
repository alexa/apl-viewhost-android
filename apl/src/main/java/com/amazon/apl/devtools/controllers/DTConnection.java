/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers;

import android.util.Log;

import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.common.Event;
import com.amazon.apl.devtools.models.common.Request;
import com.amazon.apl.devtools.models.common.Response;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.error.ErrorResponse;
import com.amazon.apl.devtools.util.CommandRequestFactory;
import com.amazon.apl.devtools.util.RequestStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * DTConnection is a wrapper for a WebSocket.
 * DTConnection stores the Sessions created by a single web socket client.
 * A DTConnection can have many Sessions.
 */
public class DTConnection {
    private static final String TAG = DTConnection.class.getSimpleName();
    private final CommandRequestFactory mCommandRequestFactory;
    private final Map<String, Session> mRegisteredSessionsMap = new HashMap<>();

    public DTConnection(CommandRequestFactory commandRequestFactory) {
        Log.i(TAG, "Wrapping web socket in a connection");
        mCommandRequestFactory = commandRequestFactory;
    }

    public void registerSession(Session session) {
        mRegisteredSessionsMap.put(session.getSessionId(), session);
        Log.i(TAG, "registerSession: Connection has " + mRegisteredSessionsMap.size() +
                " sessions");
    }

    public void unregisterSession(String sessionId) {
        mRegisteredSessionsMap.remove(sessionId);
        Log.i(TAG, "unregisterSession: Connection has " + mRegisteredSessionsMap.size() +
                " sessions");
    }

    public boolean hasSession(String sessionId) {
        return mRegisteredSessionsMap.containsKey(sessionId);
    }

    public Session getSession(String sessionId) {
        return mRegisteredSessionsMap.get(sessionId);
    }

    protected void destroyRegisteredSessions() {
        Collection<Session> sessions = mRegisteredSessionsMap.values();
        for (Session session : sessions) {
            session.destroy();
        }
    }

    public void handleMessage(String message) {
        Log.i(TAG, "Message received by connection");
        try {
            JSONObject obj = new JSONObject(message);
            Request<? extends Response> commandRequest =
                    mCommandRequestFactory.createCommandRequest(obj, this);
            // Passing a callback to send a response after executing
            commandRequest.execute(this::sendResponse);
        } catch (DTException e) {
            Log.e(TAG, "Error creating command request", e);
            Response errorResponse = new ErrorResponse(e);
            sendResponse(errorResponse);
        } catch (JSONException e) {
            Log.e(TAG, "Error deserializing command request", e);
            // Sending error response with id of 0 because the invalid request might not have an id
            Response errorResponse = new ErrorResponse(0, DTError.INVALID_COMMAND.getErrorCode(),
                    DTError.INVALID_COMMAND.getErrorMsg());
            sendResponse(errorResponse);
        }
    }

    private <TResponse extends Response> void sendResponse(TResponse response, RequestStatus requestStatus) {
        if (requestStatus.getExecutionStatus() == RequestStatus.ExecutionStatus.FAILED) {
            final DTError error = requestStatus.getError();
            Response errorResponse = new ErrorResponse(requestStatus.getId(), error.getErrorCode(), error.getErrorMsg());
            sendResponse(errorResponse);
            return;
        }

        sendResponse(response);
    }

    protected <TResponse extends Response> void sendResponse(TResponse response) {
        //no op
    }

    public <TEvent extends Event> void sendEvent(TEvent event) {
        //no op
    }
}
