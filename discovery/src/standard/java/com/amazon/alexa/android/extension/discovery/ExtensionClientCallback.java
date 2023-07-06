/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

/**
 * L3 Client.
 * This abstract class represents a single client to a remote service.  Concrete implementations
 * of this class are application specific.
 * @deprecated Appropriate ExtensionProxy should be used.
 */
@Deprecated
public class ExtensionClientCallback implements ExtensionMultiplexClient.ConnectionCallback {

    final int mID = ExtensionMultiplexClient.randomConnectionID();

    /**
     * The unique identifier of this callback.  Used for message routing.  Implementation
     * should use {@link ExtensionMultiplexClient#randomConnectionID()}.
     *
     * @return Callback identifier.
     */
    @Override
    public int getID() {
        return mID;
    }

    /**
     * The IPC connection handshake is complete.  2-way communication is available for the extension.
     *
     * @param extensionURI The extension this callback was registered for.
     */
    @Override
    public void onConnect(String extensionURI) {

    }

    /**
     * The IPC connection handshake to the service has been closed by the service.
     *
     * @param extensionURI The extension this callback was registered for.
     * @param message      Readable message.
     */
    @Override
    public void onConnectionClosed(String extensionURI, String message) {

    }

    /**
     * The IPC connection has failed.  This may be a result of IPC error
     * or the server rejecting the handshake.
     *
     * @param extensionURI The extension this callback was registered for.
     * @param errorCode    Reason for connection close.
     * @param message      Readable message.
     */
    @Override
    public void onConnectionFailure(String extensionURI, int errorCode, String message) {

    }

    /**
     * A message has been sent by the extension.
     *
     * @param extensionURI The extension this callback was registered for.
     * @param message      The message.
     */
    @Override
    public void onMessage(String extensionURI, String message) {

    }

    /**
     * A message sent to the extension could not be parsed.
     *
     * @param extensionURI  The extension this callback was registered for.
     * @param errorCode     Reason for the message failure.
     * @param message       Readable message.
     * @param failedPayload Response payload.
     */
    @Override
    public void onMessageFailure(String extensionURI, int errorCode, String message, String failedPayload) {

    }

    /**
     * A resource with a unique identifier is requested by the extension
     *
     * @param extensionURI URI associated with the extension
     * @param resourceId   Unique identifier associated with the resource
     */
    @Override
    public void onRequestResource(String extensionURI, String resourceId) {

    }
}
