/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers.impl;

import android.util.Log;
import com.amazon.apl.devtools.controllers.IDTServer;
import com.amazon.apl.devtools.util.CommandRequestFactory;
import com.amazon.apl.devtools.util.DependencyContainer;

import java.net.InetSocketAddress;

public final class DTServer implements IDTServer {

    private static final String TAG = DTServer.class.getSimpleName();
    private static final int TIMEOUT_IN_SECONDS = 5;
    private static DTServer sInstance;

    private final CommandRequestFactory mCommandRequestFactory;
    private final DependencyContainer mDependencyContainer;
    private final DTServerData mDTServerData;
    private DTWebSocketServer mWebSocketServer;

    private DTServer() {
        mDependencyContainer = DependencyContainer.getInstance();
        mCommandRequestFactory = mDependencyContainer.getCommandRequestFactory();
        mDTServerData = DTServerData.getInstance();
        mWebSocketServer = null;
    }

    public static DTServer getInstance() {
        if (sInstance == null) {
            sInstance = new DTServer();
        }
        return sInstance;
    }

    /**
     * Starts the server.
     *
     * @param portNumber The port number to start the server on.
     */
    public void start(int portNumber) {
        Log.i(TAG, "startServer: starting the server");
        mDTServerData.setPortNumber(portNumber);
        InetSocketAddress webSocketAddress = new InetSocketAddress(portNumber);
        mWebSocketServer = new DTWebSocketServer(mCommandRequestFactory, webSocketAddress);
        mWebSocketServer.start();
    }

    /**
     * Stops the server and does any necessary clean up.
     */
    public void stop() {
        try {
            mDTServerData.reset();
            mDependencyContainer.getTargetCatalog().cleanup();
            mWebSocketServer.stop(TIMEOUT_IN_SECONDS);
            Log.i(TAG, "onDestroy: DTWebSocketServer successfully closed");
        } catch (InterruptedException e) {
            // Re-interrupt the thread
            Thread.currentThread().interrupt();
            Log.e(TAG, "DTWebSocketServer fail to close", e);
            // Wrap the InterruptedException and rethrow
            throw new RuntimeException(e);
        }
    }
}