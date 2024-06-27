/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers;

public interface IDTServer {
    /**
     * Starts the server.
     *
     * @param portNumber The port number to start the server on.
     */
    void start(final int portNumber);

    /**
     * Stops the server and does any necessary clean up.
     */
    void stop();
}
