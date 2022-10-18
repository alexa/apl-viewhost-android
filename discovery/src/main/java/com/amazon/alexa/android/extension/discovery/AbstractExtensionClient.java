/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;

/**
 * This abstract class represents a single client to an extension.
 * @deprecated Appropriate ExtensionProxy should be used.
 */
@Deprecated
public  class AbstractExtensionClient implements ExtensionMultiplexClient.ConnectionCallback {
    private static final String TAG = "AbstractExtensionClient";

    private final int mRoutingID = ExtensionMultiplexClient.randomConnectionID();
    private final ExtensionMultiplexClient mClient;

    private ExtensionMultiplexClient.IMultiplexClientConnection mConnection;

    protected AbstractExtensionClient(ExtensionMultiplexClient client) {
        mClient = client;
    }

    protected boolean connect(final String serviceId) {
        mConnection = mClient.connect(serviceId, this);
        return (mConnection != null);
    }


    public void disconnect(final String uri, final String message) {
        if (mConnection != null) {
            mClient.disconnect(uri, this, message);
        }
    }

    /**
     * Called when the document executes an extension command.
     */
    public void sendMessage(final String message) {
        try {
            if (mConnection != null) {
                mConnection.send(this, new ActivityDescriptor("", new SessionDescriptor(""), ""), message);
            } else {
                Log.w(AbstractExtensionClient.TAG, "Calling command when service is not connected");
            }
        } catch (final RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendResourceAvailable(Surface surface, Rect rect, String resourceID) {
        try {
            if (mConnection != null) {
                mConnection.resourceAvailable(this, new ActivityDescriptor("", new SessionDescriptor(""), ""), surface, rect, resourceID);
            } else {
                Log.w(AbstractExtensionClient.TAG, "Calling command when service is not connected");
            }
        } catch (final RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendResourceUnavailable(String resourceID) {
        try {
            if (mConnection != null) {
                mConnection.resourceUnavailable(this, new ActivityDescriptor("", new SessionDescriptor(""), ""), resourceID);
            } else {
                Log.w(AbstractExtensionClient.TAG, "Calling command when service is not connected");
            }
        } catch (final RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * The unique identifier of this callback.  Used for message routing.  Implementation
     * should use {@link ExtensionMultiplexClient#randomConnectionID()}.
     *
     * @return Callback identifier.
     */
    @Override
    public int getID() {
        return mRoutingID;
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
     * A resource has been requested by an extension.
     * @param extensionURI URI associated with the extension
     * @param resourceId   Unique identifier associated with the resource (Surface).
     */
    @Override
    public void onRequestResource(String extensionURI, String resourceId) {

    }

}
