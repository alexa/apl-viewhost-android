/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.app.Service;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Surface;

import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.ConnectionID;
import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.IMultiplexServiceConnection;
import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.getInstance;

/**
 * This abstract class represents an extension.
 * @deprecated Use {@link AbstractExtensionServiceV2} instead.
 */
@SuppressWarnings("deprecation")
@Deprecated
public abstract class AbstractExtensionService extends Service implements ExtensionMultiplexService.ConnectionCallback {

    protected IMultiplexServiceConnection mMultiplexService;

    public AbstractExtensionService() {
        super();
    }

    public abstract String getName();


    @Override
    public IBinder onBind(final Intent intent) {
        if (mMultiplexService == null) {
            mMultiplexService = getInstance().connect(this);
        }
        return mMultiplexService;
    }


    @Override
    public void onDestroy() {
        // remove the connections
        getInstance().disconnect(this, "Service Destroyed: " + getName());
    }

    protected void sendMessage(ConnectionID connectionID, int clientID, final String message) {
        try {
            mMultiplexService.send(connectionID, clientID, message);
        } catch (final RemoteException e) {
            e.printStackTrace();
            onClientFail(connectionID, clientID, e.getMessage());
        }
    }

    protected void sendResourceRequest(ConnectionID connectionID, int clientID, final String resourceID) {
        try {
            mMultiplexService.requestResource(connectionID, clientID, resourceID);
        } catch (final RemoteException e) {
            e.printStackTrace();
            onClientFail(connectionID, clientID, e.getMessage());
        }
    }

    protected void sendBroadcast(final String message) {
        try {
            mMultiplexService.sendBroadcast(message);
        } catch (final RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void sendFailure(ConnectionID connectionID, int clientID, int errorCode, String error,
                               final String failedPayload) {
        try {
            mMultiplexService.sendFailure(connectionID, clientID, errorCode, error, failedPayload);
        } catch (final RemoteException e) {
            e.printStackTrace();
            onClientFail(connectionID, clientID, e.getMessage());
        }
    }

    protected void disconnect(final String message) {
        getInstance().disconnect(this, message);
    }


    /**
     * Called when a client has experienced a RemoteException.
     *
     * @param connectionID The unique ID of the connection.
     * @param clientID     The unique ID of the client.
     * @param error        The failure message.
     */
    protected void onClientFail(ConnectionID connectionID, int clientID, String error) {

    }


    /**
     * The IPC connection handshake is complete.  2-way communication is available for the
     * extension. Each client using the connection will have a unique ID.
     *
     * @param connectionID  The unique connection identifier.
     * @param configuration Extension configuration.
     */
    @Override
    public void onConnect(ConnectionID connectionID, String configuration) {

    }

    /**
     * The IPC connection handshake to the service has been closed by the service.
     *
     * @param connectionID The unique connection identifier.
     * @param message      Readable message.
     */
    @Override
    public void onConnectionClosed(ConnectionID connectionID, String message) {

    }

    /**
     * A connection has failed.  This is caused by death of the server or client.
     *
     * @param connectionID The unique connection identifier.
     * @param error        Reason for the failure.
     * @param message      Readable message.
     */
    @Override
    public void onConnectionFailure(ConnectionID connectionID, int error, String message) {

    }

    /**
     * A message has been sent by a client.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     * @param message      The message.
     */
    @Override
    public void onMessage(ConnectionID connectionID, int clientID, String message) {

    }

    /**
     * A client has lost focus.  This is usually triggered by the window manager of the
     * calling process.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onFocusLost(ConnectionID connectionID, int clientID) {

    }

    /**
     * A client gained focus.  This is usually triggered by the window manager of the
     * calling process.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     */
    @Override
    public void onFocusGained(ConnectionID connectionID, int clientID) {

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
     * A client has resumed operation after being paused.  This is usually triggered by the calling
     * process being moved to the foreground, and becoming visible.  The extension should restore
     * state to reflect all current data values.
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
    public void onExit(ConnectionID connectionID, int clientID) {

    }

    /**
     * A resource which has been requested by the extension is now available.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     * @param surface      Resource/Surface associated with unique resourceID.
     * @param rect         Frame dimensions of the resource.
     * @param resourceID   Unique Resource ID.
     */
    @Override
    public void onResourceAvailable(ConnectionID connectionID, int clientID, Surface surface, Rect rect, String resourceID) {

    }

    /**
     * A resource which has been requested by the extension is not available.
     *
     * @param connectionID The unique connection identifier.
     * @param clientID     The unique client identifier.
     * @param resourceID   Unique Resource ID.
     */
    @Override
    public void onResourceUnavailable(ConnectionID connectionID, int clientID, String resourceID) {

    }
}
