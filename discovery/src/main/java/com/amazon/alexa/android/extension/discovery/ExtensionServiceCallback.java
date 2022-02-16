/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.graphics.Rect;
import android.view.Surface;

/**
 * No-op implementation of the Extension Service Connection Callback.
 */
@SuppressWarnings("deprecation")
public class ExtensionServiceCallback implements ExtensionMultiplexService.ConnectionCallback {


    /**
     * The IPC connection handshake is complete.  2-way communication is available for the
     * extension. Each client using the connection will have a unique ID.
     *
     * @param connectionID  The unique connection identifier.
     * @param configuration Extension configuration.
     */
    @Override
    public void onConnect(ExtensionMultiplexService.ConnectionID connectionID, String configuration) {

    }

    /**
     * The IPC connection handshake to the service has been closed by the client.
     *
     * @param connectionID The unique connection identifier.
     * @param message      Readable message.
     */
    @Override
    public void onConnectionClosed(ExtensionMultiplexService.ConnectionID connectionID, String message) {

    }

    /**
     * A connection has failed.  This is caused by death of the server or client.
     *
     * @param connectionID The connectionID from {@link ExtensionMultiplexService}, null if the
     *                     client could not be identified.
     * @param error        Reason for the failure.
     * @param message      Readable message.
     */
    @Override
    public void onConnectionFailure(ExtensionMultiplexService.ConnectionID connectionID, int error, String message) {

    }

    /**
     * A message has been sent by a client.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     * @param message      The message.
     */
    @Override
    public void onMessage(ExtensionMultiplexService.ConnectionID connectionID, int clientID, String message) {

    }

    /**
     * A client has lost focus.  This is usually triggered by the window manager of the
     * calling process.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onFocusLost(ExtensionMultiplexService.ConnectionID connectionID, int clientID) {

    }

    /**
     * A client gained focus.  This is usually triggered by the window manager of the
     * calling process.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onFocusGained(ExtensionMultiplexService.ConnectionID connectionID, int clientID) {

    }

    /**
     * The resource requested by the server is available.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     * @param surface      Resource/Surface associated with unique resourceID.
     * @param rect         Frame dimensions of the resource.
     * @param resourceID   Unique Resource ID.
     */
    @Override
    public void onResourceAvailable(ExtensionMultiplexService.ConnectionID connectionID, int clientID, Surface surface, Rect rect, String resourceID) {

    }

    /**
     * The resource requested by the server is not available.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     * @param resourceID   Unique Resource ID.
     */
    @Override
    public void onResourceUnavailable(ExtensionMultiplexService.ConnectionID connectionID, int clientID, String resourceID) {

    }

    /**
     * A client has been paused.  This is usually triggered by the calling process being moved
     * to the background, and is no longer visible.  Messages to a paused client will be ignored,
     * and state updates will no longer be sent to the client.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onPause(ExtensionMultiplexService.ConnectionID connectionID, int clientID) {

    }

    /**
     * A client gained focus.  This is usually triggered by the calling process being moved
     * to the foreground, and becoming visible.  The extension should restore state to reflect
     * all current data values.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onResume(ExtensionMultiplexService.ConnectionID connectionID, int clientID) {

    }

    /**
     * A client exited.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onExit(ExtensionMultiplexService.ConnectionID connectionID, int clientID) {

    }
}
