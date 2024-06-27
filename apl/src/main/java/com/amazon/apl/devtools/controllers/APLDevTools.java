/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.devtools.controllers.impl.NoOpDTServer;

/**
 * This class provide a easy way to integrate with devtools.
 */
public class APLDevTools {
    private final IDTServer mDTServer;
    private int mPortNumber;

    public APLDevTools() {
        if (BuildConfig.DEBUG) {
            mDTServer = DTServer.getInstance();
        } else {
            mDTServer = new NoOpDTServer();
        }
    }

    /**
     * Set the specific port number to start the server on.
     * @param portNumber The port number.
     * @return the same {@link APLDevTools} instance.
     */
    public APLDevTools setPort(int portNumber) {
        mPortNumber = portNumber;
        return this;
    }

    /**
     * Starts the devtools server with the given configurations.
     */
    public void start() {
        mDTServer.start(mPortNumber);
    }

    /**
     * Stops the devtools server.
     */
    public void stop() {
        mDTServer.stop();
    }
}
