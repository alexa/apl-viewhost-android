package com.amazon.alexaext;

import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base delegate that holds common functionality for {@link RemoteExtensionProxy}.
 */
abstract class BaseRemoteProxyDelegate implements ExtensionMultiplexClient.ConnectionCallback {
    private static final String TAG = "BaseRemoteProxyDelegate";

    private final int mID = ExtensionMultiplexClient.randomConnectionID();
    @NonNull
    protected final ExtensionMultiplexClient mMultiplexClient;
    @NonNull
    protected final Map<String, String> mRegistrations = new HashMap<>();

    // Messages from extension
    protected final List<String> mInboundMessages = new ArrayList<>();

    private InternalMessageAction mOnInternalMessageAction;

    // Connected means the extensions is bound, see onConnect().
    protected boolean mConnected;

    /**
     * Registered means that RegisterSuccess message has been passed to discovery.
     * <p>
     * For regular in-process extensions {@link BaseRemoteProxyDelegate} this happens after the extension
     * service is bound (onConnect), registration message is passed to the extension and
     * RegisterSuccess is received back.
     * <p>
     * For deferred extensions {@link DeferredRemoteProxyDelegate} that are bound only after onRenderDocument
     * is invoked the pre-defined RegisterSuccess message is passed right on registration attempt,
     * before connection is happened. This results in onRegisteredInternal being called before
     * onConnect and acutal registration message is passed to the extension.
     */
    protected boolean mRegistered;

    @Nullable
    private ExtensionMultiplexClient.IMultiplexClientConnection mConnection;

    interface InternalMessageAction {
        boolean act(@NonNull String uri, String message, boolean registered);
    }

    BaseRemoteProxyDelegate(@NonNull final ExtensionMultiplexClient multiplexClient) {
        mMultiplexClient = multiplexClient;
    }

    void setOnInternalMessageAction(@NonNull final InternalMessageAction action) {
        mOnInternalMessageAction = action;
    }

    abstract boolean onProxyInitialize(@NonNull final String uri);

    /**
     * Creates a connection to the service by a given extension uri.
     *
     * @param uri the extension uri
     */
    protected synchronized void connect(@NonNull final String uri) {
        if (mConnection == null) {
            mConnection = mMultiplexClient.connect(uri, this);
        }
    }

    void disconnect(@NonNull final String uri, final String message) {
        mMultiplexClient.disconnect(uri, this, message);
        reset();
    }

    @CallSuper
    synchronized boolean onRequestRegistration(@NonNull final String uri, final String request) {
        if (!mRegistered) {
            if (!mConnected) {
                // Delay as can be processed only after connection
                mRegistrations.put(uri, request);
                return true;
            }
            return sendMessage(uri, request);
        }

        return false;
    }

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

    synchronized void onRegisteredInternal(@NonNull final String uri) {
        // Now we can transfer other messages.
        mRegistered = true;

        for (String message : mInboundMessages) {
            onMessageInternal(uri, message);
        }
    }

    boolean onMessageInternal(@NonNull final String uri, final String message) {
        if (mOnInternalMessageAction == null) {
            throw new IllegalStateException("MessageAction not provided");
        }

        return mOnInternalMessageAction.act(uri, message, mRegistered);
    }

    void onUnregisteredInternal(@NonNull final String uri) {
        disconnect(uri, "Un-registered");
    }

    /**
     * The IPC connection handshake is complete.  2-way communication is available for the extension.
     *
     * @param extensionURI The extension this callback was registered for.
     */
    @Override
    @CallSuper
    public synchronized void onConnect(String extensionURI) {
        mConnected = true;

        if (mRegistrations.containsKey(extensionURI)) {
            sendMessage(extensionURI, mRegistrations.get(extensionURI));
            mRegistrations.remove(extensionURI);
        }
    }

    synchronized void onResourceReadyInternal(@NonNull final ResourceHolder resourceHolder) {
        if (!mConnected) {
            return;
        }

        try {
            final SurfaceHolder surface = (SurfaceHolder) resourceHolder.getFacet(SurfaceHolder.class);
            mConnection.resourceAvailable(this, surface.getSurface(),
                    surface.getSurfaceFrame(), resourceHolder.resourceId());
        } catch (IllegalStateException ie) {
            Log.w(TAG, "Cannot Send Resource: " + ie.getMessage());
            ie.printStackTrace();
        } catch (RemoteException re) {
            Log.w(TAG, "Calling extension when service is not connected");
            re.printStackTrace();
        }
    }

    @CallSuper
    synchronized boolean sendMessage(@NonNull final String uri, final String message) {
        if (!mConnected) {
            Log.w(TAG, "Calling command when service is not connected");
            return false;
        }

        try {
            mConnection.send(this, message);
            return true;
        } catch (final RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }


    /**
     * The IPC connection handshake to the service has been closed by the service.
     *
     * @param uri     The extension this callback was registered for.
     * @param message Readable message.
     */
    @Override
    public void onConnectionClosed(@NonNull final String uri, final String message) {
        reset();
    }

    /**
     * The IPC connection has failed.  This may be a result of IPC error
     * or the server rejecting the handshake.
     *
     * @param uri       The extension this callback was registered for.
     * @param errorCode Reason for connection close.
     * @param message   Readable message.
     */
    @Override
    public void onConnectionFailure(@NonNull final String uri, final int errorCode, final String message) {
        reset();
    }

    @Override
    public synchronized void onMessage(@NonNull final String uri, final String message) {
        if (!onMessageInternal(uri, message)) {
            mInboundMessages.add(message);
        }
    }

    /**
     * A message sent to the extension could not be parsed.
     *
     * @param uri           The extension this callback was registered for.
     * @param errorCode     Reason for the message failure.
     * @param message       Readable message.
     * @param failedPayload Response payload.
     */
    @Override
    public void onMessageFailure(@NonNull final String uri, final int errorCode,
                                 final String message, final String failedPayload) {
    }

    /**
     * Invoked when controlling instance gained platform focus.
     */
    @Deprecated
    @CallSuper
    synchronized void onFocusGained(@NonNull final String uri) {
        if (!mConnected) {
            return;
        }

        try {
            mConnection.setFocusGain(this);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Unable to gain focus.", e);
        }
    }

    /**
     * Invoked when controlling instance lost platform focus.
     */
    @Deprecated
    @CallSuper
    synchronized void onFocusLost() {
        if (!mConnected) {
            return;
        }

        try {
            mConnection.setFocusLost(this);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Unable to loose focus", e);
        }
    }

    private synchronized void reset() {
        mConnected = false;
        mRegistered = false;
        mConnection = null;
    }
}
