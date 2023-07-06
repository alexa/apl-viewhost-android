/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexServiceV2.ConnectionID;
import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexServiceV2.IMultiplexServiceConnection;
import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexServiceV2.getInstance;

import android.app.Service;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Surface;

import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;

/**
 * This abstract class represents an extension.
 */
public abstract class AbstractExtensionServiceV2 extends Service implements ExtensionMultiplexServiceV2.ConnectionCallback {
    protected IMultiplexServiceConnection mMultiplexService;

    public AbstractExtensionServiceV2() {
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

    protected void sendMessage(
            final ConnectionID connectionID,
            final int clientID,
            final ActivityDescriptor activity,
            final String message) {
        try {
            mMultiplexService.send(connectionID, clientID, activity, message);
        } catch (final RemoteException e) {
            e.printStackTrace();
            onClientFail(connectionID, clientID, e.getMessage());
        }
    }

    protected void sendFailure(
            final ConnectionID connectionID,
            final int clientID,
            final ActivityDescriptor activity,
            final int errorCode,
            final String error,
            final String failedPayload) {
        try {
            mMultiplexService.sendFailure(connectionID, clientID, activity, errorCode, error, failedPayload);
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
    protected void onClientFail(
            final ConnectionID connectionID,
            final int clientID,
            final String error) {
    }

    @Override
    public void onConnect(final ConnectionID connectionID, final String configuration) {
    }

    @Override
    public void onConnectionClosed(final ConnectionID connectionID, final String message) {
    }

    @Override
    public void onConnectionFailure(
            final ConnectionID connectionID,
            final int error,
            final String message) {
    }

    @Override
    public void onMessage(
            final ConnectionID connectionID,
            final int clientID,
            final ActivityDescriptor activity,
            final String message) {
    }

    @Override
    public void onResourceAvailable(
            final ConnectionID connectionID,
            final int clientID,
            final ActivityDescriptor activity,
            final Surface surface,
            final Rect rect,
            final String resourceID) {
    }

    @Override
    public void onResourceUnavailable(
            final ConnectionID connectionID,
            final int clientID,
            final ActivityDescriptor activity,
            final String resourceID) {
    }

    @Override
    public void onRegistered(
            final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity) {
    }

    @Override
    public void onUnregistered(
            final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity) {
    }

    @Override
    public void onSessionStarted(
            final ConnectionID connectionID, final int clientID, final SessionDescriptor session) {
    }

    @Override
    public void onSessionEnded(
            final ConnectionID connectionID, final int clientID, final SessionDescriptor session) {
    }

    @Override
    public void onForeground(
            final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity) {
    }

    @Override
    public void onBackground(
            final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity) {
    }

    @Override
    public void onHidden(
            final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity) {
    }
}
