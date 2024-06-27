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
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSession;

/**
 * DTConnection is a wrapper for a WebSocket.
 * DTConnection stores the Sessions created by a single web socket client.
 * A DTConnection can have many Sessions.
 */
public final class DTConnection implements WebSocket {
    private static final String TAG = DTConnection.class.getSimpleName();
    private final CommandRequestFactory mCommandRequestFactory;
    private final WebSocket mWebSocket;
    private final Map<String, Session> mRegisteredSessionsMap = new HashMap<>();

    public DTConnection(CommandRequestFactory commandRequestFactory,
                        WebSocket webSocket) {
        Log.i(TAG, "Wrapping web socket in a connection");
        mCommandRequestFactory = commandRequestFactory;
        mWebSocket = webSocket;
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

    private void destroyRegisteredSessions() {
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

    private <TResponse extends Response> void sendResponse(TResponse response) {
        try {
            String responseStr = response.toJSONString();
            Log.i(TAG, "Sending web socket message: " + responseStr);
            send(responseStr);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing response", e);
        }
    }

    public <TEvent extends Event> void sendEvent(TEvent event) {
        try {
            String eventStr = event.toJSONString();
            Log.i(TAG, "Sending web socket message: " + eventStr);
            send(eventStr);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing event", e);
        }
    }

    @Override
    public void close(int code, String message) {
        Log.i(TAG, "Closing connection");
        mWebSocket.close(code, message);
        destroyRegisteredSessions();
    }

    @Override
    public void close(int code) {
        mWebSocket.close(code);
    }

    @Override
    public void close() {
        mWebSocket.close();
    }

    @Override
    public void closeConnection(int code, String message) {
        mWebSocket.closeConnection(code, message);
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        if (isOpen()) {
            mWebSocket.send(text);
        } else {
            Log.w(TAG, "Connection closed, ignoring request to send message.");
        }
    }

    @Override
    public void send(ByteBuffer bytes) throws IllegalArgumentException, NotYetConnectedException {
        mWebSocket.send(bytes);
    }

    @Override
    public void send(byte[] bytes) throws IllegalArgumentException, NotYetConnectedException {
        mWebSocket.send(bytes);
    }

    @Override
    public void sendFrame(Framedata framedata) {
        mWebSocket.sendFrame(framedata);
    }

    @Override
    public void sendFrame(Collection<Framedata> frames) {
        mWebSocket.sendFrame(frames);
    }

    @Override
    public void sendPing() throws NotYetConnectedException {
        mWebSocket.sendPing();
    }

    @Override
    public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {
        mWebSocket.sendFragmentedFrame(op, buffer, fin);
    }

    @Override
    public boolean hasBufferedData() {
        return mWebSocket.hasBufferedData();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return mWebSocket.getRemoteSocketAddress();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return mWebSocket.getLocalSocketAddress();
    }

    @Override
    public boolean isOpen() {
        return mWebSocket.isOpen();
    }

    @Override
    public boolean isClosing() {
        return mWebSocket.isClosing();
    }

    @Override
    public boolean isFlushAndClose() {
        return mWebSocket.isFlushAndClose();
    }

    @Override
    public boolean isClosed() {
        return mWebSocket.isClosed();
    }

    @Override
    public Draft getDraft() {
        return mWebSocket.getDraft();
    }

    @Override
    public ReadyState getReadyState() {
        return mWebSocket.getReadyState();
    }

    @Override
    public String getResourceDescriptor() {
        return mWebSocket.getResourceDescriptor();
    }

    @Override
    public <T> void setAttachment(T attachment) {
        mWebSocket.setAttachment(attachment);
    }

    @Override
    public <T> T getAttachment() {
        return mWebSocket.getAttachment();
    }

    @Override
    public boolean hasSSLSupport() {
        return mWebSocket.hasSSLSupport();
    }

    @Override
    public SSLSession getSSLSession() throws IllegalArgumentException {
        return mWebSocket.getSSLSession();
    }

    @Override
    public IProtocol getProtocol() {
        return mWebSocket.getProtocol();
    }
}
