/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers;

import android.util.Log;

import com.amazon.apl.devtools.util.CommandRequestFactory;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * DTWebSocketServer receives web socket messages from web socket clients.
 * Then, DTWebSocketServer delegates handling the requests to a DTConnection.
 * A DTWebSocketServer can have many DTConnections.
 */
public final class DTWebSocketServer extends WebSocketServer {
    private static final String TAG = DTWebSocketServer.class.getSimpleName();
    private static final int RETRY_INTERVAL_IN_MILLISECONDS = 5000;
    private final CommandRequestFactory mCommandRequestFactory;
    private final Map<WebSocket, DTConnection> mConnections = new HashMap<>();
    private final DTServer mDTServer;

    public DTWebSocketServer(CommandRequestFactory commandRequestFactory,
                             InetSocketAddress host) {
        super(host);
        Log.i(TAG, "Creating web socket server");

        setReuseAddr(true);
        mCommandRequestFactory = commandRequestFactory;
        mDTServer = DTServer.getInstance();
    }

    private void addDTConnection(WebSocket conn) {
        DTConnection connection = new DTConnection(mCommandRequestFactory, conn);
        mConnections.put(conn, connection);
    }

    private DTConnection getDTConnection(WebSocket conn) {
        if (!mConnections.containsKey(conn)) {
            Log.e(TAG, "Error getting connection because the web socket key doesn't exist");
        }
        return mConnections.get(conn);
    }

    private DTConnection removeDTConnection(WebSocket conn) {
        if (!mConnections.containsKey(conn)) {
            Log.e(TAG, "Error removing connection because the web socket key doesn't exist");
        }
        return mConnections.remove(conn);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i(TAG, "Opening web socket");
        addDTConnection(conn);
        Log.i(TAG, "Number of web socket connections: " + mConnections.size());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String message = "reason: " + reason + ", remote: " + remote;
        Log.i(TAG, "Closing web socket, code: " + code + ", " + message);
        DTConnection connection = removeDTConnection(conn);
        connection.close(code, message);
        Log.i(TAG, "Number of web socket connections: " + mConnections.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.i(TAG, "Receiving web socket message: " + message);
        DTConnection connection = getDTConnection(conn);
        connection.handleMessage(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "Web socket error occurred", ex);
        DTServerData serverData = DTServerData.getInstance();
        if (ex instanceof BindException) {
            if (serverData.getRetryCount() < DTServerData.MAX_RETRIES) {
                websocketRetry();
                serverData.incrementRetryCount();
            } else {
                Log.e(TAG, "Max retries reached. Giving up.");
            }
        }

    }

    //reconnect web-socket when BindException happened
    void websocketRetry() {
        try {
            // without sleep, app will keep trying the restart till it overflows
            Thread.sleep(RETRY_INTERVAL_IN_MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Web socket error occurred " + e);
        }
        // restart the websocket when the port is released
        mDTServer.start(DTServerData.getInstance().getPortNumber());
    }

    @Override
    public void onStart() {
        Log.i(TAG, "Starting web socket server");
    }
}