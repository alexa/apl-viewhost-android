/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers;

/**
 * Any APL runtime can use this class to start and stop the server in order to have the
 * dev tools protocol enable/disable.
 */
public class DTServer {

    private static final String TAG = DTServer.class.getSimpleName();
    private static final int TIMEOUT_IN_SECONDS = 5;
    private static DTServer sInstance;

    public DTServer() {}

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
        //no op
    }

    /**
     * Stops the server and does any necessary clean up.
     */
    public void stop() {
        // no op
    }
}
