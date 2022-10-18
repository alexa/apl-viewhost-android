/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexa.android.extension.discovery;

import com.amazon.alexa.android.extension.discovery.ExtensionDiscovery.ExtensionPresence;
import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.IExtension;
import com.amazon.alexaext.SessionDescriptor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Surface;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * L2 Client.
 * This class is responsible for the client/extension connection handshake, connection transactions,
 * multiplexing calls to a service.  The connection handshake establishes 2-way communication between
 * client and server.  Messages may be sent/received once establishes.
 * May wrapL3 to facilitate communication.
 * @deprecated Use {@link com.amazon.alexaext.DiscoveryExtensionsProvider#getExtension(String)} provided proxy instead.
 */
@SuppressWarnings("deprecation")
@Deprecated
public class ExtensionMultiplexClient {
    private static final String TAG = "ExtensionMultiplexClnt";
    private static final boolean DEBUG = false;
    private final ExtensionBinder mBinder = new ExtensionBinder();

    /**
     * The Android Context extension biding is running in.
     */
    public interface ExtensionContext {
        /**
         * @return The Android Context.
         */
        Context getContext();
    }

    /**
     * A Connection to a service. A service may represent multiple Extensions, and an Extension may
     * have multiple clients (receivers).  Each client is give a unique routingID so that it may
     * be uniquely identified.
     */
    public interface IMultiplexClientConnection {
        /**
         * @return The unique identifier for this connection.
         */
        int connectionID();

        /**
         * Message send to the service.  This message uses the callback ID so the server
         * may identify the message sender.
         *
         * @param callback The connection client sending this message.
         * @param activity ActivityDescriptor.
         * @param message  The message.
         * @throws RemoteException Binder remote-invocation error.
         */
        void send(ConnectionCallback callback, ActivityDescriptor activity, String message) throws RemoteException;

        /**
         * Message send to the service.  This message uses the callback ID so the server
         * may identify the message sender.
         *
         * @param callback The connection client sending this message.
         * @param message  The message.
         * @throws RemoteException Binder remote-invocation error.
         * @deprecated Use the other variant of send that specifies an ActivityDescriptor
         */
        @Deprecated
        void send(ConnectionCallback callback, String message) throws RemoteException;

        /**
         * Window management has transferred focus away from the client. This is a signal for the
         * extension to optionally pause data and event updates.
         *
         * @param callback The connection client.
         * @throws RemoteException Binder remote-invocation error.
         * @deprecated Rely on new activity-based lifecycle APIs.
         */
        @Deprecated
        void setFocusLost(ConnectionCallback callback) throws RemoteException;

        /**
         * Window management has given focus to the client. This is a signal for the extension
         * to optionally resume all data and event updates.
         *
         * @param callback The connection client.
         * @throws RemoteException Binder remote-invocation error.
         * @deprecated Rely on new activity-based lifecycle APIs.
         */
        @Deprecated
        void setFocusGain(ConnectionCallback callback) throws RemoteException;

        /**
         * Window management has moved the client to the background. This is a signal for the
         * extension to pause all data and event updates.
         *
         * @param callback The connection client.
         * @throws RemoteException Binder remote-invocation error.
         * @deprecated Rely on new activity-based lifecycle APIs.
         */
        @Deprecated
        void pause(ConnectionCallback callback) throws RemoteException;

        /**
         * Window management moved the client to the foreground. This is a signal for the extension
         * to resume all data and event updates.
         *
         * @param callback The connection client.
         * @throws RemoteException Binder remote-invocation error.
         * @deprecated Rely on new activity-based lifecycle APIs.
         */
        @Deprecated
        void resume(ConnectionCallback callback) throws RemoteException;

        /**
         * The resource which was requested by server is available.
         *
         * @param callback   The connection client.
         * @param surface    Handle to a surface/resource with a given unique ID
         * @param rect       Frame details associated with the resource.
         * @param resourceID Unique identifier of the associated surface/resource.
         * @throws RemoteException Binder remote-invocation error.
         */
        default void resourceAvailable(ConnectionCallback callback, Surface surface, Rect rect, String resourceID) throws RemoteException {}

        /**
         * The resource which was requested by server is not available.
         *
         * @param callback   The connection client.
         * @param resourceID Unique identifier of the associated surface/resource.
         * @throws RemoteException Binder remote-invocation error.
         */
        default void resourceUnavailable(ConnectionCallback callback, String resourceID) throws RemoteException {}

        /**
         * The resource which was requested by server is available.
         *
         * @param callback   The connection client.
         * @param activity   ActivityDescriptor.
         * @param surface    Handle to a surface/resource with a given unique ID
         * @param rect       Frame details associated with the resource.
         * @param resourceID Unique identifier of the associated surface/resource.
         * @throws RemoteException Binder remote-invocation error.
         */
        default void resourceAvailable(ConnectionCallback callback, ActivityDescriptor activity, Surface surface, Rect rect, String resourceID) throws RemoteException {}

        /**
         * The resource which was requested by server is not available.
         *
         * @param callback   The connection client.
         * @param activity   ActivityDescriptor.
         * @param resourceID Unique identifier of the associated surface/resource.
         * @throws RemoteException Binder remote-invocation error.
         */
        default void resourceUnavailable(ConnectionCallback callback, ActivityDescriptor activity, String resourceID) throws RemoteException {}

        /**
         * @see IExtension#onRegistered(ActivityDescriptor)
         */
        default void onRegistered(ConnectionCallback callback, ActivityDescriptor session) throws RemoteException {}

        /**
         * @see IExtension#onUnregistered(ActivityDescriptor)
         */
        default void onUnregistered(ConnectionCallback callback, ActivityDescriptor session) throws RemoteException {}

        /**
         * @see IExtension#onSessionStarted(SessionDescriptor)
         */
        default void onSessionStarted(ConnectionCallback callback, SessionDescriptor session) throws RemoteException {}

        /**
         * @see IExtension#onSessionEnded(SessionDescriptor)
         */
        default void onSessionEnded(ConnectionCallback callback, SessionDescriptor session) throws RemoteException {}

        /**
         * @see IExtension#onForeground(ActivityDescriptor)
         */
        default void onForeground(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {}

        /**
         * @see IExtension#onBackground(ActivityDescriptor)
         */
        default void onBackground(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {}

        /**
         * @see IExtension#onHidden(ActivityDescriptor)
         */
        default void onHidden(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {}
    }


    /**
     * Generating secure UID.
     */
    @SuppressLint("InlinedApi")
    private static final SecureRandom sGenerator = new SecureRandom(
            SecureRandom.getSeed((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    ? Integer.BYTES : 32));


    /**
     * @return A random routing ID for an extension client.
     */
    public static int randomConnectionID() {
        int id = 0;
        while (id == 0) {
            id = sGenerator.nextInt();
        }
        return id;

    }


    /**
     * Callback from the service connection used for {@link #connect(String, ConnectionCallback)}.
     * Notification order is maintained based on connect order.
     *
     * @deprecated use {@link ExtensionClientCallback}. This class will be for internal
     * use only and made package private.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    public interface ConnectionCallback {

        /**
         * The unique identifier of this callback.  Used for message routing.  Implementation
         * should use {@link #randomConnectionID()}.
         *
         * @return Callback identifier.
         */
        int getID();

        /**
         * The IPC connection handshake is complete.  2-way communication is available for the extension.
         *
         * @param extensionURI The extension this callback was registered for.
         */
        void onConnect(String extensionURI);

        /**
         * The IPC connection handshake to the service has been closed by the service.
         *
         * @param extensionURI The extension this callback was registered for.
         * @param message      Readable message.
         */
        void onConnectionClosed(String extensionURI, String message);

        /**
         * The handshake with the service could not be completed, The service will be automatically
         * disconnected. This may be a result of a service failure, the service rejecting the connection,
         * or a client-service version incompatibility.
         */
        int FAIL_HANDSHAKE = -100;

        /**
         * The service died.
         */
        int FAIL_SERVER_DIED = -200;

        /**
         * The IPC connection has failed.  This may be a result of IPC error
         * or the server rejecting the handshake.
         *
         * @param extensionURI The extension this callback was registered for.
         * @param errorCode    Reason for connection close.
         * @param message      Readable message.
         */
        void onConnectionFailure(String extensionURI, int errorCode, String message);

        /**
         * A message has been sent by the extension.
         *
         * @param extensionURI The extension this callback was registered for.
         * @param message      The message.
         */
        void onMessage(String extensionURI, String message);

        /**
         * A message sent to the extension could not be parsed.
         *
         * @param extensionURI  The extension this callback was registered for.
         * @param errorCode     Reason for the message failure.
         * @param message       Readable message.
         * @param failedPayload Response payload.
         */
        void onMessageFailure(String extensionURI, int errorCode, String message, String failedPayload);

        /**
         * A resource has been requested by the extension.
         *
         * @param extensionURI URI associated with the extension.
         * @param resourceId   Unique Identifier associated with the resource.
         */
        default void onRequestResource(String extensionURI, String resourceId) {}

        /// V2 support
        /**
         * A message has been sent by the extension.
         *
         * @param activity      ActivityDescriptor.
         * @param message      The message.
         */
        default void onMessage(ActivityDescriptor activity, String message) {}

        /**
         * A message sent to the extension could not be parsed.
         *
         * @param activity      ActivityDescriptor.
         * @param errorCode     Reason for the message failure.
         * @param message       Readable message.
         * @param failedPayload Response payload.
         */
        default void onMessageFailure(ActivityDescriptor activity, int errorCode, String message, String failedPayload) {}
    }


    // The context for this client, used for ServiceConnection binding/unbinding
    private ExtensionContext mContext;

    /**
     * @param context Android Context.
     */
    public ExtensionMultiplexClient(final ExtensionContext context) {
        // private constructor for singleton
        mContext = context;
    }

    // Connection monitor, monitors death of client callbacks and notifies the runtime of failures.
    private final ConnectionMonitorList<ClientConnection, String> mConnections =
            new ConnectionMonitorList<ClientConnection, String>() {
                // A client connection has died and has been removed from this list.
                // Note: "Callback" here is the death monitor, and implemented by ClientConnection
                @Override
                public void onDied(final ClientConnection connection, final String extensionURI) {
                    if (DEBUG) Log.d(TAG, "Service has died: " + extensionURI);

                    if (connection != null) {
                        // notify connection callback that the server died
                        connection.disconnectOnFailure(ConnectionCallback.FAIL_SERVER_DIED,
                                "The service has died:" + extensionURI);
                    }
                }
            };


    /**
     * Create an IPC connection for a service.  All messages are processed by a {@link Handler}.
     * on the calling thread.  If the calling thread does not provide a looper, a new
     * {@link HandlerThread} is used instead of the calling thread.
     *
     * @param extensionURI The service identifier.
     * @param callback     Callback for connection events.
     * @return A connection to the service, null if not found.
     */
    public IMultiplexClientConnection connect(@NonNull final String extensionURI,
                                              @NonNull final ConnectionCallback callback) {

        if (isKilled()) {
            throw new IllegalStateException("Cannot reuse connection after call to kill()");
        }

        return connect(extensionURI, null, callback);
    }


    /**
     * Create an IPC connection for a service.  All messages are processed by a {@link Handler}.
     * on the calling thread.  If the calling thread does not provide a looper, a new
     * {@link HandlerThread} is used instead of the calling thread.
     *
     * @param extensionURI  The service identifier.
     * @param configuration Connection configuration, may be null.
     * @param callback      Callback for connection events.
     * @return A connection to the service, null if not found.
     */
    public IMultiplexClientConnection connect(@NonNull final String extensionURI,
                                              final String configuration,
                                              @NonNull final ConnectionCallback callback) {

        if (isKilled()) {
            throw new IllegalStateException("Cannot reuse connection after call to kill()");
        }

        Looper myLooper = Looper.myLooper();
        if (myLooper == null) {
            Log.w(TAG, "There is no Looper for the current thread, creating a handler thread");
            // no looper for this thread, create a handler thread
            HandlerThread thread = new HandlerThread("ExtensionHandlerThread");
            thread.start();
            myLooper = thread.getLooper();
        }

        return connect(extensionURI, configuration, myLooper, callback, false);
    }

    /**
     * Create an IPC connection to a service.
     * <p>
     * This method is no longer valid after a call to {@link #kill()}. A new ExtensionMultiplexClient
     * must created.  This is required because the Android death monitor for all connections is
     * disabled on kill.
     * <p>
     * Async on a specific Looper is not supported by Android in versions earlier than Pie.
     *
     * @param extensionURI The service identifier.
     * @param looper       The looper for messaging processing, may be null.
     * @param callback     Callback for connection events.
     * @param async        If true {@link IMultiplexClientConnection} messages are processed
     *                     directly on the calling thread.  When false, a {@link Handler} is
     *                     constructed with the current threads associated {@link Looper}.
     * @return A connection to the service, null if not found.
     */
    public IMultiplexClientConnection connect(@NonNull final String extensionURI,
                                              final String configuration, final Looper looper,
                                              @NonNull final ConnectionCallback callback,
                                              final boolean async) {

        if (isKilled()) {
            throw new IllegalStateException("Cannot reuse connection after call to kill()");
        }

        //noinspection ConstantConditions
        if (extensionURI == null || callback == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (null != looper && async) {
                throw new UnsupportedOperationException("Async message processing on a specific "
                        + "Looper is not on Android versions earlier than Pie.");
            }
        }


        ClientConnection connection;
        boolean requiresBinding = false;

        // Connections are multiplexed, reuse existing instance
        synchronized (mConnections) {

            connection = mConnections.get(extensionURI);
            if (connection == null) {
                if (DEBUG) Log.d(TAG, "Creating connection: " + extensionURI);
                connection = new ClientConnection(extensionURI, looper, async, configuration);
                requiresBinding = true;
            }

            // The callback needs to be added before the binding is attempted. This allows
            // connection status events (success/fail) to be passed to the callback.
            // if the service is already bound, the callback is notified immediately
            connection.registerCallback(callback);

            // connect to the service if needed
            if (requiresBinding) {
                if (mBinder.bind(mContext.getContext(), extensionURI, connection)) {
                    if (DEBUG) Log.d(TAG, "Registering connection: " + extensionURI);
                    mConnections.register(connection, extensionURI);
                } else {
                    Log.e(TAG, "Service not available");
                    return null;
                }
            }
        }

        return connection;
    }

    /**
     * Provides {@link ExtensionPresence} that defines how the extension should be bound:
     *
     * {@link ExtensionPresence#PRESENT}
     * means an extension can be connected {@link ExtensionMultiplexClient#connect},
     *
     * {@link ExtensionPresence#NOT_PRESENT}
     * means extension is not available for the runtime and {@link ExtensionMultiplexClient#connect}
     * will have no effect,
     *
     * {@link ExtensionPresence#DEFERRED}
     * means extension has declared that it's connection can be deferred
     *
     * @param uri URI of the extension
     * @return {@link ExtensionPresence}
     */
    public ExtensionDiscovery.ExtensionPresence hasExtension(final String uri) {
        return ExtensionDiscovery.getInstance(mContext.getContext()).hasExtension(uri);
    }


    /**
     * @param uri URI of an extension
     * @return extension defition for the URI
     */
    public String getExtensionDefinition(final String uri) {
        return ExtensionDiscovery.getInstance(mContext.getContext()).getExtensionDefinition(uri);
    }

    /**
     * Disconnect an extension client.
     *
     * @param extensionURI The service identifier.
     * @param callback     Callback used in {@link #connect(String, ConnectionCallback)}
     * @param message      Reason for disconnect.
     * @return True if the connection existed and disconnect was attempted.
     */
    public boolean disconnect(final String extensionURI, final ConnectionCallback callback,
                              final String message) {
        if (DEBUG) Log.d(TAG, "disconnect: " + extensionURI);

        Log.i(TAG, String.format("Closing connection: %s, reason: %s", extensionURI, message));
        boolean result = false;
        synchronized (mConnections) {
            // find the connection and remove the callback
            ClientConnection connection = mConnections.get(extensionURI);
            if (null != connection) {
                result = connection.unregisterCallback(callback);
                if (connection.mCallbacks.isEmpty()) {
                    clearConnection(connection);
                }
            }
        }
        return result;
    }


    /**
     * Clear a unused connection.  Called when unused, or on connection failure.
     *
     * @param connection The connection.
     */
    private void clearConnection(ClientConnection connection) {
        // Close the connection if there are no callbacks
        mConnections.unregister(connection);
        connection.close();
        mBinder.unbind(mContext.getContext(), connection.mExtensionURI);
    }


    /**
     * @return true if a previous call to {#kill()} was made.
     */
    public boolean isKilled() {
        return mConnections.isKilled();
    }


    /**
     * Kill all clients and terminate connections.  Typically used on context exit.  This instance
     * of ExtensionMultiplexClient can no longer be used after this call.
     */
    public void kill() {
        if (isKilled()) return;
        synchronized (mConnections) {
            int N = mConnections.beginBroadcast(); // makes a copy of collection
            for (int i = 0; i < N; i++) {
                ClientConnection connection = mConnections.getBroadcastItem(i);
                String extensionURI = String.valueOf(mConnections.getBroadcastCookie(i));
                connection.close();
                mBinder.unbind(mContext.getContext(), extensionURI);
            }
            mConnections.finishBroadcast();
            mConnections.kill();
        }
        mContext = null;
    }

    /**
     * @return The number of open connections.
     */
    @VisibleForTesting
    int getConnectionCount() {
        return mConnections.getRegisteredCallbackCount();
    }


    /**
     * @return the internal extension binder.
     */
    @VisibleForTesting
    ExtensionBinder getBinder() {
        return mBinder;
    }


    /**
     * A Connection to an extension. A service may represent multiple Extensions, and an Extension may
     * have multiple clients.  Each client is give a unique routingID across all clients.
     */
    @VisibleForTesting
    class ClientConnection extends L2_IRemoteClient.Stub
            implements ExtensionBinder.ConnectionCallback, IMultiplexClientConnection {

        // Unique connection identifier for this client;
        private final int mConnectionID;
        // The extensionURI
        private final String mExtensionURI;

        // Registered callbacks for this connection. A callback is identified by URI, by routing id.
        private final Map<Integer, ConnectionCallback> mCallbacks
                = Collections.synchronizedMap(new LinkedHashMap<>());
        // Connection configuration details
        private final String mConfiguration;

        // The connected service
        private boolean mHasService = false;
        private L2_IRemoteService mServiceV1;
        private L2_IRemoteServiceV2 mServiceV2;
        // The connection has been accepted by service
        private final AtomicBoolean mAccept = new AtomicBoolean(false);
        // Message Handler
        private final Handler mHandler;
        // Identify as closed for testing
        final CountDownLatch mBindingFailTest = new CountDownLatch(1);

        // Simulated activity descriptor for shimming V1 clients
        // @deprecated Remove once there are no more V1 clients
        private final ActivityDescriptor mDummyActivityDescriptor;

        ClientConnection(final String extensionURI, final Looper looper, final boolean async,
                         final String configuration) {
            mExtensionURI = extensionURI;
            mConfiguration = configuration;

            mConnectionID = randomConnectionID();

            Handler handler = null;
            if (!async && looper == null) {
                // default use current threads looper for sync messaging
                handler = new Handler();
            } else if (!async) {
                // use the specified looper for sync messaging
                handler = new Handler(looper);
            } else if (looper != null) {
                // Messages sent to an async handler are guaranteed to be ordered with respect
                // to one another, but not necessarily with respect to messages from other Handlers.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    handler = Handler.createAsync(looper);
                }
                // async on a specific looper is not supported before Pie
            }
            mHandler = handler;

            // The following "dummy" activity descriptor is a shim for V1 clients
            // * If a V1 client connects to a V1 service, it will not be used, but
            //   is only passed around internally within this class.
            // * If a V1 client connects to a V2 service, it will actually be
            //   given to the V2 service as the activity descriptor. In that case,
            //   the activity descriptor is simply analogous to a connection ID.
            final String activityId = UUID.randomUUID().toString();
            final String sessionId = UUID.randomUUID().toString();
            mDummyActivityDescriptor = new ActivityDescriptor(
                extensionURI,
                new SessionDescriptor(sessionId),
                activityId);
        }

        /**
         * @return The unique id for this connection within this pid.
         */
        @Override
        public int connectionID() {
            return mConnectionID;
        }


        /**
         * Register a callback for this connection.
         *
         * @param callback The callback.
         */
        void registerCallback(final ConnectionCallback callback) {
            synchronized (mCallbacks) {
                // save this callback with a routing ID.
                final int routingID = callback.getID();
                mCallbacks.put(routingID, callback);
                if (mHasService && mAccept.get()) {
                    if (null != mHandler) {
                        mHandler.post(() -> notifyConnect(callback));
                    } else {
                        notifyConnect(callback);
                    }
                }
            }
        }


        /**
         * Unregister a callback for this connection.
         *
         * @param callback The callback.
         * @return {@code true} if the callback had been previously registered
         */
        boolean unregisterCallback(final ConnectionCallback callback) {
            synchronized (mCallbacks) {
                ConnectionCallback result = mCallbacks.remove(callback.getID());
                try {
                    if (mHasService) {
                        if (mServiceV1 != null) {
                            mServiceV1.L2_onExit(connectionID(), callback.getID());
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Connection close failed", e);
                }
                return result != null;
            }
        }


        /**
         * Close connection.
         */
        void close() {
            try {
                if (mHasService) {
                    if (mServiceV1 != null) {
                        mServiceV1.L2_connectionClosed(this, "Exit");
                    } else if (mServiceV2 != null) {
                        mServiceV2.L2_connectionClosed(this, "Exit");
                    }
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Connection close failed", e);
            }
        }


        /**
         * @return The number of registered callbacks
         */
        int getRegisteredCallbackCount() {
            return mCallbacks.size();
        }


        /**
         * @return The connected service, possibly null if the connection is incomplete or failed.
         */
        @VisibleForTesting
        L2_IRemoteService getService() {
            return mServiceV1;
        }

        /**
         * Notify callbacks that the connection has failed and disconnects from the service.
         *
         * @param errorCode Cause of the error.
         * @param error     Human readable string
         */
        private void disconnectOnFailure(final int errorCode, final String error) {
            Log.w(TAG, "disconnectOnFailure:" + errorCode + " " + error);
            // close the connection
            mServiceV1 = null;
            mServiceV2 = null;
            mHasService = false;

            // notify clients
            synchronized (mCallbacks) {
                for (final ConnectionCallback callback : mCallbacks.values()) {
                    if (null != mHandler) {
                        mHandler.post(() -> notifyConnectFailure(callback, errorCode, error));
                    } else {
                        notifyConnectFailure(callback, errorCode, error);
                    }
                }
                mCallbacks.clear();
            }
            clearConnection(this);
        }

        /**
         * Notify a single callback of a connection error.
         *
         * @param callback  The callback.
         * @param errorCode Cause of the error.
         * @param error     Human readable string
         */
        private void notifyConnectFailure(final ConnectionCallback callback,
                                          final int errorCode, final String error) {
            try {
                callback.onConnectionFailure(mExtensionURI, errorCode, error);
            } catch (Exception e) {
                Log.w(TAG, "Unhandled connection failure for service:" + mExtensionURI);
            }
        }


        /**
         * L1 IPC binding to service is successful.  The L2 connection handshake is initiated.
         */
        @Override
        public void bindingSuccess(final IBinder service) {
            // initiate the two-way communication by sending this client to the server
            try {
                String descriptor = service.getInterfaceDescriptor();
                Log.i(TAG, "INTERFACE_DESCRIPTOR:" + descriptor);
                if (descriptor.equals("com.amazon.alexa.android.extension.discovery.L2_IRemoteService")) {
                    // the server has been bound, save a reference
                    mServiceV1 = L2_IRemoteService.Stub.asInterface(service);
                    mServiceV1.L2_connect(this, mConfiguration);
                    mHasService = true;
                } else if (descriptor.equals("com.amazon.alexa.android.extension.discovery.L2_IRemoteServiceV2")) {
                    mServiceV2 = L2_IRemoteServiceV2.Stub.asInterface(service);
                    mServiceV2.L2_connect(this, mConfiguration);
                    mHasService = true;
                } else {
                    disconnectOnFailure(ConnectionCallback.FAIL_HANDSHAKE,
                            "Client - Server transaction incompatibility");
                }
            } catch (final RemoteException e) {
                // The service has crashed during handshake; we can count on being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
                Log.e(TAG, "Binding Failure", e);
            }
        }


        /**
         * L1 IPC binding to the service failed, disconnect.
         */
        @Override
        public void bindingFailure(final int errorCode, final String error) {
            mBindingFailTest.countDown();
            int code = (errorCode == FAIL_DISCONNECTED)
                    ? ConnectionCallback.FAIL_SERVER_DIED : ConnectionCallback.FAIL_HANDSHAKE;
            disconnectOnFailure(code, error);
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public boolean L2_supportsTransactVersion(final int expectedVersion) throws RemoteException {
            return (expectedVersion == TRANSACT_VERSION || expectedVersion == 2);
        }


        @SuppressWarnings("RedundantThrows")
        @Override
        public int L2_connectionID() throws RemoteException {
            return mConnectionID;
        }


        /**
         * IPC connection handshake is complete.  2-way communication is available for all
         * Extensions supported by the service
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public void L2_connectionAccept() throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_connectionSuccess");

            mAccept.set(true);
            // notify the L3 callback,
            synchronized (mCallbacks) {
                for (final ConnectionCallback callback : mCallbacks.values()) {
                    if (null != mHandler) {
                        mHandler.post(() -> notifyConnect(callback));
                    } else {
                        notifyConnect(callback);
                    }
                }
            }
        }

        /**
         * Notify a single callback of connection.
         *
         * @param callback The Callback.
         */
        private void notifyConnect(final ConnectionCallback callback) {
            try {
                callback.onConnect(mExtensionURI);
            } catch (Exception e) {
                Log.e(TAG, "Connection notification failed: " + mExtensionURI
                        + " clientID: " + callback.getID());
            }
        }


        /**
         * IPC connection handshake to the service failed. Communication to all Extensions supported
         * by the service is unavailable.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public void L2_connectionReject(final int errorCode, final String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_connectionFailure: " + errorCode);
            disconnectOnFailure(ConnectionCallback.FAIL_HANDSHAKE, message);
        }


        /**
         * IPC connection handshake to the service has been closed by the service.  Communication
         * to all Extensions supported by the service is closed.
         */
        @SuppressWarnings({"RedundantThrows", "checkstyle:LineLength"})
        @Override
        public void L2_connectionClosed(final String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_connectionClosed: " + message);

            // disconnect all clients
            synchronized (mCallbacks) {
                for (final ConnectionCallback callback : mCallbacks.values()) {
                    if (null != mHandler) {
                        mHandler.post(() -> notifyConnectClosed(callback, message));
                    } else {
                        notifyConnectClosed(callback, message);
                    }
                    if (!disconnect(mExtensionURI, callback, message)) {
                        Log.w(TAG, "Failed to disconnect from: " + mExtensionURI);
                    }
                }
            }
        }


        /**
         * Notify a single client of a closed connection.
         *
         * @param callback The client.
         * @param message  Readable message.
         */
        private void notifyConnectClosed(final ConnectionCallback callback, final String message) {
            try {
                callback.onConnectionClosed(mExtensionURI, message);
            } catch (Exception e) {
                Log.w(TAG, "Failed to notify client of closed connection:" + mExtensionURI);
            }
        }


        /**
         * Message received from the service.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public void L2_receive(final int routingID, final String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_receive: " + message);

            if (mHandler != null) {
                mHandler.post(() -> notifyMessage(routingID, message));
            } else {
                notifyMessage(routingID, message);
            }
        }


        /**
         * Internal receive processing.
         *
         * @param routingID The target callback id.
         * @param message   The Message.
         */
        private void notifyMessage(final int routingID, final String message) {

            // notify the L3 callback
            try {
                ConnectionCallback callback = mCallbacks.get(routingID);
                if (callback != null) {
                    callback.onMessage(mExtensionURI, message);
                } else {
                    // may have requested disconnect before looper ran
                    Log.w(TAG, "Failed to find client id = " + message);
                }
            } catch (Exception e) {
                Log.e(TAG, "onReceive callback error.", e);
            }
        }

        /**
         * Message received from the service to be broadcast to all callbacks.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public void L2_receiveBroadcast(final String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_receive: " + message);

            if (mHandler != null) {
                mHandler.post(() -> notifyBroadcast(message));
            } else {
                notifyBroadcast(message);
            }
        }

        /**
         * Internal broadcast processing.
         *
         * @param message The Message
         */
        public void notifyBroadcast(final String message) {

            // notify each L3 callback
            synchronized (mCallbacks) {
                for (Integer routingID : mCallbacks.keySet()) {
                    notifyMessage(routingID, message);
                }
            }
        }

        /**
         * Shim for legacy V1 method
         */
        @Deprecated
        @Override
        public void send(final ConnectionCallback callback, final String message) throws RemoteException {
            send(callback, mDummyActivityDescriptor, message);
        }

        /**
         * Shim for legacy V1 method
         */
        @Deprecated
        @Override
        public void setFocusLost(final ConnectionCallback callback) throws RemoteException {
            onBackground(callback, mDummyActivityDescriptor);
        }

        /**
         * Shim for legacy V1 method
         */
        @Deprecated
        @Override
        public void setFocusGain(final ConnectionCallback callback) throws RemoteException {
            onForeground(callback, mDummyActivityDescriptor);
        }

        /**
         * Shim for legacy V1 method
         */
        @Deprecated
        @Override
        public void resourceAvailable(ConnectionCallback callback, Surface surface, Rect rect, String resourceID) throws RemoteException {
            resourceAvailable(callback, mDummyActivityDescriptor, surface, rect, resourceID);
        }

        /**
         * Shim for legacy V1 method
         */
        @Deprecated
        @Override
        public void resourceUnavailable(ConnectionCallback callback, String resourceID) throws RemoteException {
            resourceUnavailable(callback, mDummyActivityDescriptor, resourceID);
        }

        /**
         * Message send to the service.
         */
        @Override
        public void send(final ConnectionCallback callback, ActivityDescriptor activity, final String message) throws RemoteException {
            if (null == callback || null == message)
                throw new RemoteException("No routing identifiers");

            if (mServiceV1 != null) {
                L2_send(callback.getID(), message);
            } else if (mServiceV2 != null) {
                L2_sendV2(callback.getID(), activity, message);
            }
        }

        /**
         * Message send to the service.
         */
        @Override
        public void L2_send(final int routingID, final String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_send: " + message);

            try {
                if (mServiceV1 != null) {
                    mServiceV1.L2_receive(mConnectionID, routingID, message);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling send without service connection");
                throw (e);
            }
        }

        /**
         * Internal focus lost to the service.
         */
        @Override
        @Deprecated
        public void L2_setFocusLost(final int routingID) throws RemoteException {}

        /**
         * Internal focus gained to the service.
         */
        @Override
        @Deprecated
        public void L2_setFocusGained(final int routingID) throws RemoteException {}


        /**
         * Message pause to the service.
         */
        @Override
        public void pause(final ConnectionCallback callback) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");
            L2_pause(callback.getID());
        }

        /**
         * Internal pause to the service.
         */
        @Override
        public void L2_pause(final int routingID) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_pause");

            try {
                if (mServiceV1 != null) {
                    mServiceV1.L2_onPause(mConnectionID, routingID);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling send without service connection");
                throw (e);
            }
        }


        /**
         * Message resume  to the service.
         */
        @Override
        public void resume(final ConnectionCallback callback) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");
            L2_resume(callback.getID());
        }

        @Override
        public void resourceAvailable(ConnectionCallback callback, ActivityDescriptor activity, Surface surface, Rect rect, String resourceID) throws RemoteException {
            if (null == callback || null == surface ) {
                throw new RemoteException("No routing identifiers");
            }

            if (mServiceV1 != null) {
                L2_resourceAvailable(callback.getID(), surface, rect, resourceID);
            } else if (mServiceV2 != null) {
                L2_resourceAvailableV2(callback.getID(), activity, surface, rect, resourceID);
            }
        }

        @Override
        public void resourceUnavailable(ConnectionCallback callback, ActivityDescriptor activity, String resourceID) throws RemoteException {
            if (null == callback) {
                throw new RemoteException("No routing identifiers");
            }

            if (mServiceV1 != null) {
                L2_resourceUnavailable(callback.getID(), resourceID);
            } else if (mServiceV2 != null) {
                L2_resourceUnavailableV2(callback.getID(), activity, resourceID);
            }
        }

        /**
         * Internal resumed to the service.
         */
        @Override
        public void L2_resume(final int routingID) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_resumed");

            try {
                if (mServiceV1 != null) {
                    mServiceV1.L2_onResume(mConnectionID, routingID);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling send without service connection");
                throw (e);
            }
        }

        /**
         * Message send to the extension.
         *
         * @param routingID     The target client.
         * @param errorCode     Error Identifier.
         * @param error         Readable error.
         * @param failedMessage The payload that failed.
         */
        @Override
        public void L2_messageFailure(final int routingID, final int errorCode,
                                      final String error, final String failedMessage)
                throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_messageFailure: " + failedMessage);

            if (null != mHandler) {
                boolean result = mHandler.post(() -> notifyMessageFailure(routingID, errorCode,
                        error, failedMessage));
                if (!result) {
                    throw new RemoteException("Remote message queue failed");
                }
            } else {
                notifyMessageFailure(routingID,
                        errorCode, error, failedMessage);
            }
        }

        /**
         * Internal fail processing.
         *
         * @param errorCode     Error Identifier.
         * @param error         Readable error.
         * @param failedMessage The payload that failed.
         */
        private void notifyMessageFailure(final int routingID, final int errorCode,
                                          final String error, final String failedMessage) {

            ConnectionCallback callback = mCallbacks.get(routingID);
            if (callback != null) {
                try {
                    callback.onMessageFailure(mExtensionURI, errorCode, error, failedMessage);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling message failure:" + error);
                }
            }
        }

        @Override
        public void L2_onRequestResource(int routingID, String resourceId) throws RemoteException {
            if (DEBUG) Log.d(TAG, "onRequestResource: " + resourceId);

            if (mHandler != null) {
                mHandler.post(() -> notifyResourceRequest(routingID, resourceId));
            } else {
                notifyResourceRequest(routingID, resourceId);
            }
        }

        private void notifyResourceRequest(final int routingID, final String resourceId) {
            try {
                ConnectionCallback callback = mCallbacks.get(routingID);
                if (callback != null) {
                    callback.onRequestResource(mExtensionURI, resourceId);
                }
            } catch (Exception e) {
                Log.e(TAG, "notifyResourceRequest error: ", e);
            }
        }

        @Override
        public void L2_resourceAvailable(int routingID, Surface surface, Rect rect, String resourceID) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onResourceAvailable: resourceID=" + resourceID);
                if (mServiceV1 != null) {
                    mServiceV1.L2_onResourceAvailable(mConnectionID, routingID, surface, rect, resourceID);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void L2_resourceUnavailable(int routingID, String resourceID) throws RemoteException {
            try {
                if (mServiceV1 != null) {
                    mServiceV1.L2_onResourceUnavailable(mConnectionID, routingID, resourceID);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource unavailable without service connection");
                throw (e);
            }
        }

        /// V2 Specific handlers

        @Override
        public void L2_receiveV2(int routingID, ActivityDescriptor activity, String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_receiveV2: " + message);

            if (mHandler != null) {
                mHandler.post(() -> notifyMessageV2(routingID, activity, message));
            } else {
                notifyMessageV2(routingID, activity, message);
            }
        }

        private void notifyMessageV2(final int routingID, ActivityDescriptor activity, final String message) {
            // notify the L3 callback
            try {
                ConnectionCallback callback = mCallbacks.get(routingID);
                if (callback != null) {
                    callback.onMessage(activity, message);
                } else {
                    // may have requested disconnect before looper ran
                    Log.w(TAG, "Failed to find client id = " + message);
                }
            } catch (Exception e) {
                Log.e(TAG, "onReceive callback error.", e);
            }
        }

        @Override
        public void L2_messageFailureV2(int routingID, ActivityDescriptor activity, int errorCode, String error, String failedMessage) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_messageFailure: " + failedMessage);

            if (null != mHandler) {
                boolean result = mHandler.post(() -> notifyMessageFailureV2(routingID, activity,
                        errorCode, error, failedMessage));
                if (!result) {
                    throw new RemoteException("Remote message queue failed");
                }
            } else {
                notifyMessageFailureV2(routingID, activity, errorCode, error, failedMessage);
            }
        }

        private void notifyMessageFailureV2(final int routingID, ActivityDescriptor activity,
                                            final int errorCode, final String error,
                                            final String failedMessage) {
            ConnectionCallback callback = mCallbacks.get(routingID);
            if (callback != null) {
                try {
                    callback.onMessageFailure(activity, errorCode, error, failedMessage);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling message failure:" + error);
                }
            }
        }

        @Override
        public void L2_sendV2(int routingID, ActivityDescriptor activity, String message) throws RemoteException {
            if (DEBUG) Log.d(TAG, "L2_sendV2: " + message);

            try {
                if (mServiceV2 != null) {
                    mServiceV2.L2_receive(mConnectionID, routingID, activity, message);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling send without service connection");
                throw (e);
            }
        }

        @Override
        public void L2_resourceAvailableV2(int routingID, ActivityDescriptor activity, Surface surface, Rect rect, String resourceID) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onResourceAvailableV2: resourceID=" + resourceID);
                if (mServiceV2 != null) {
                    mServiceV2.L2_onResourceAvailable(mConnectionID, routingID, activity, surface, rect, resourceID);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void L2_resourceUnavailableV2(int routingID, ActivityDescriptor activity, String resourceID) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onResourceUnavailableV2: resourceID=" + resourceID);
                if (mServiceV2 != null) {
                    mServiceV2.L2_onResourceUnavailable(mConnectionID, routingID, activity, resourceID);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onRegistered(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");

            L2_onRegisteredV2(callback.getID(), activity);
        }

        @Override
        public void L2_onRegisteredV2(int routingID, ActivityDescriptor activity) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onRegisteredV2: activity=" + activity.getActivityId());
                if (mServiceV2 != null) {
                    mServiceV2.L2_onRegistered(mConnectionID, routingID, activity);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onUnregistered(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {
            if (null == callback) {
                throw new RemoteException("No routing identifiers");
            }

            if (mServiceV1 != null) {
                disconnect(activity.getURI(), callback, "Un-registered");
            }

            L2_onUnregisteredV2(callback.getID(), activity);
        }

        @Override
        public void L2_onUnregisteredV2(int routingID, ActivityDescriptor activity) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onUnregisteredV2: activity=" + activity.getActivityId());
                if (mServiceV2 != null) {
                    mServiceV2.L2_onUnregistered(mConnectionID, routingID, activity);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onSessionStarted(ConnectionCallback callback, SessionDescriptor session) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");

            L2_onSessionStartedV2(callback.getID(), session);
        }

        @Override
        public void L2_onSessionStartedV2(int routingID, SessionDescriptor session) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onSessionStartedV2: activity=" + session.getId());
                if (mServiceV2 != null) {
                    mServiceV2.L2_onSessionStarted(mConnectionID, routingID, session);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onSessionEnded(ConnectionCallback callback, SessionDescriptor session) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");

            L2_onSessionEndedV2(callback.getID(), session);
        }

        @Override
        public void L2_onSessionEndedV2(int routingID, SessionDescriptor session) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onSessionEndedV2: activity=" + session.getId());
                if (mServiceV2 != null) {
                    mServiceV2.L2_onSessionEnded(mConnectionID, routingID, session);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onForeground(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");

            L2_onForegroundV2(callback.getID(), activity);
        }

        @Override
        public void L2_onForegroundV2(int routingID, ActivityDescriptor activity) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onForegroundV2: activity=" + activity.getActivityId());
                if (mServiceV1 != null) {
                    mServiceV1.L2_onFocusGained(mConnectionID, routingID);
                } else if (mServiceV2 != null) {
                    mServiceV2.L2_onForeground(mConnectionID, routingID, activity);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onBackground(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");

            L2_onBackgroundV2(callback.getID(), activity);
        }

        @Override
        public void L2_onBackgroundV2(int routingID, ActivityDescriptor activity) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onBackgroundV2: activity=" + activity.getActivityId());
                if (mServiceV1 != null) {
                    mServiceV1.L2_onFocusLost(mConnectionID, routingID);
                } else if (mServiceV2 != null) {
                    mServiceV2.L2_onBackground(mConnectionID, routingID, activity);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }

        @Override
        public void onHidden(ConnectionCallback callback, ActivityDescriptor activity) throws RemoteException {
            if (null == callback)
                throw new RemoteException("No routing identifiers");

            L2_onHiddenV2(callback.getID(), activity);
        }

        @Override
        public void L2_onHiddenV2(int routingID, ActivityDescriptor activity) throws RemoteException {
            try {
                if(DEBUG) Log.d(TAG, "onHiddenV2: activity=" + activity.getActivityId());
                if (mServiceV1 != null) {
                    mServiceV1.L2_onFocusLost(mConnectionID, routingID);
                } else if (mServiceV2 != null) {
                    mServiceV2.L2_onHidden(mConnectionID, routingID, activity);
                }
            } catch (final RemoteException e) {
                Log.e(TAG, "calling resource available without service connection");
                throw (e);
            }
        }
    }
}
