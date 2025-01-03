/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

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

import java.util.Objects;

/**
 * L2 Service.
 * This class is responsible for message events, connection request/refusals, and multiplexing.
 * May wrapL3 to facilitate communication.
 * @deprecated Use {@link ExtensionMultiplexServiceV2} instead.
 */
@Deprecated
public final class ExtensionMultiplexService {

    /**
     * Debug tag.
     */
    private static final String TAG = "ExtensionMultiplexSvc";
    /**
     * Singleton.
     */
    private static final ExtensionMultiplexService sInstance = new ExtensionMultiplexService();
    /**
     * Connection monitor, monitors death of client connections and notifies the runtime of failures.
     */
    private final ConnectionMonitorList<ServiceConnection, Object> mConnections =
            new ConnectionMonitorList<ServiceConnection, Object>() {

                // A client connection has died and has been removed from this list.
                // Note: "Callback" here is the death monitor, and implemented by ServiceConnection
                @Override
                public void onCallbackDied(final ServiceConnection connection, final Object cookie) {
                    if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, "Service has died: " + cookie);

                    if (connection != null) {
                        // notify connection callbacks that the server died
                        connection.disconnectOnFailure(null,
                                ConnectionCallback.FAIL_SERVER_DIED, "The service has died");
                    }
                }
            };

    private ExtensionMultiplexService() {
        // private constructor for singleton
        super();
    }

    /**
     * @return Single instance.
     */
    public static ExtensionMultiplexService getInstance() {
        return ExtensionMultiplexService.sInstance;
    }

    /**
     * Create an IPC connection for a service.  All messages are processed by a {@link Handler}.
     * on the calling thread.  If the calling thread does not provide a looper,  a new
     * {@link HandlerThread} is used instead of the calling thread.
     *
     * @param callback Callback for connection events.
     * @return A connection to the service.
     */
    public IMultiplexServiceConnection connect(final ConnectionCallback callback) {

        Looper myLooper = Looper.myLooper();
        if (null == myLooper) {
            // no looper for this thread, create a handler thread
            HandlerThread thread = new HandlerThread("ExtensionHandlerThread");
            thread.start();
            myLooper = thread.getLooper();
        }

        return connect(myLooper, callback, false);
    }

    /**
     * Create an IPC connection for a service.  Messages may be synchronized / ordered based on
     * parameters.
     * async = false and looper = null -> processed by Handler on calling threads looper.
     * async = false and looper != null -> processed by Handler using provided looper.
     * async = true  and looper != null -> processed by async handler using provided looper.
     * async = true and looper = null -> messages processed directly on calling thread
     * <p>
     * Async on a specific Looper is not supported by Android in versions earlier than Pie.
     *
     * @param looper   Used by handler for message ordering.  May be null.
     * @param callback Callback for connection events.
     * @param async    If false {@link IMultiplexServiceConnection} messages are processed
     *                 by a {@link Handler}.
     * @return A connection to the service.
     */
    public IMultiplexServiceConnection connect(final Looper looper, @NonNull final ConnectionCallback callback,
                                               final boolean async) {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (null != looper && async) {
                throw new UnsupportedOperationException("Async message processing on a specific "
                        + "Looper is not on Android versions earlier than Pie.");
            }
        }

        ServiceConnection connection;

        // Connections are multiplexed, reuse existing instance
        synchronized (mConnections) {

            connection = mConnections.get(callback);

            if (null == connection) {
                connection = new ServiceConnection(looper, callback, async);
                mConnections.register(connection, callback);
            }

        }

        return connection;

    }

    /**
     * Remove an IPC connection for a service.
     *
     * @param callback The service callback used for {@link #connect(ConnectionCallback)}
     * @param message  Readable message.
     */
    public void disconnect(final ConnectionCallback callback, final String message) {

        synchronized (mConnections) {

            ServiceConnection connection = mConnections.get(callback);
            if (connection != null) {
                mConnections.unregister(connection);
                connection.closeConnection(message);
            }
        }
    }

    /**
     * Kills all connections. All registered connections are unregistered,
     * and the list is disabled so that future calls to {@link #connect} will
     * fail.  This should be used when a Service is stopping, to prevent clients
     * from registering callbacks after it is stopped.
     *
     * @param message Readable disconnect reason.
     */
    @VisibleForTesting
    public void disconnectAll(final String message) {
        if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, message);

        synchronized (mConnections) {

            int N = mConnections.beginBroadcast();
            for (int i = 0; i < N; i++) {
                ServiceConnection connection = mConnections.getBroadcastItem(i);
                connection.closeConnection(message);
            }
            mConnections.finishBroadcast();
            mConnections.kill();
        }
    }


    /**
     * A Connection to Extension  clients.
     * <p>
     * A service may represent multiple Extensions, each with multiple
     * connections.  The connection may additionally represent multiple clients.  Each client is
     * give a unique routingID so that it may be uniquely identified.
     */
    @SuppressWarnings("checkstyle:LineLength")
    public interface IMultiplexServiceConnection extends IBinder {

        /**
         * Send a message to a single clients.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param message      The message.
         * @throws RemoteException The transaction failed
         */
        void send(ConnectionID connectionID, int clientID, String message) throws RemoteException;

        /**
         * Request a resource for a resource ID
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param resourceID   Unique identifier for the resource.
         * @throws RemoteException The transaction failed.
         */
        default void requestResource(ConnectionID connectionID, int clientID, String resourceID) throws RemoteException {}

        /**
         * Broadcast a message to all clients.
         *
         * @param message The message
         * @throws RemoteException The transaction failed.
         */
        void sendBroadcast(String message) throws RemoteException;

        /**
         * Send a message to a single clients.
         *
         * @param connectionID  The unique connection identifier.
         * @param clientID      The unique client identifier.
         * @param errorCode     The errorCode.
         * @param error         Readable message.
         * @param failedMessage The failed payload.
         * @throws RemoteException The transaction failed.
         */
        void sendFailure(ConnectionID connectionID, int clientID, int errorCode, String error,
                         String failedMessage) throws RemoteException;

    }


    /**
     * Callback from the service connection.
     * @deprecated use {@link ExtensionServiceCallback}. This class will be for internal use only
     * and made package private.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    public interface ConnectionCallback {

        /**
         * The handshake with the client could not be completed, The client will be automatically
         * disconnected. This may be a result of a client failure, the client rejecting the
         * connection, or a client-service version incompatibility.
         */
        int FAIL_HANDSHAKE = -100;
        /**
         * The client died.
         */
        int FAIL_CLIENT_DIED = -200;
        /**
         * Sent by the framework to the client when the service dies.
         */
        int FAIL_SERVER_DIED = -300;

        /**
         * The IPC connection handshake is complete.  2-way communication is available for the
         * extension. Each client using the connection will have a unique ID.
         *
         * @param connectionID  The unique ID of the client.
         * @param configuration Extension configuration.
         */
        void onConnect(ConnectionID connectionID, String configuration);

        /**
         * The IPC connection handshake to the service has been closed by the service.
         *
         * @param connectionID The unique ID of the client.
         * @param message      Readable message.
         */
        void onConnectionClosed(ConnectionID connectionID, String message);

        /**
         * A connection has failed.  This is caused by death of the server or client.
         *
         * @param connectionID The connectionID from {@link #onConnect(ConnectionID, String)}, null if the
         *                     client could not be identified.
         * @param error        Reason for the failure.
         * @param message      Readable message.
         */
        void onConnectionFailure(ConnectionID connectionID, int error, String message);

        /**
         * A message has been sent by a client.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param message      The message.
         */
        void onMessage(ConnectionID connectionID, int clientID, String message);

        /**
         * A client has lost focus.  This is usually triggered by the window manager of the
         * calling process.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         */
        void onFocusLost(ConnectionID connectionID, int clientID);

        /**
         * A client gained focus.  This is usually triggered by the window manager of the
         * calling process.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         */
        void onFocusGained(ConnectionID connectionID, int clientID);

        /**
         * A resource is available for an Extension Component. This is usually in response to a
         * ResourceRequest made by the service to the client.
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param surface      Resource/Surface associated with unique resourceID.
         * @param rect         Frame dimensions of the resource.
         * @param resourceID   Unique Resource ID.
         */
        default void onResourceAvailable(ConnectionID connectionID, int clientID, Surface surface, Rect rect, String resourceID) {}

        /**
         * A resource is not available for an Extension Component. This is usually in response to a
         * ResourceRequest made by the service to the client. In this case, the request failed.
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param resourceID   Unique Resource ID.
         */
        default void onResourceUnavailable(ConnectionID connectionID, int clientID, String resourceID) {}

        /**
         * A client has been paused.  This is usually triggered by the calling process being moved
         * to the background, and is no longer visible.  Messages to a paused client will be ignored,
         * and state updates will no longer be sent to the client.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         */
        void onPause(ConnectionID connectionID, int clientID);

        /**
         * A client has resumed operation after being paused.  This is usually triggered by the calling
         * process being moved to the foreground, and becoming visible.  The extension should restore
         * state to reflect all current data values.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         */
        void onResume(ConnectionID connectionID, int clientID);

        /**
         * A client exited.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         */
        void onExit(ConnectionID connectionID, int clientID);
    }


    /**
     * Represents a unique connection to to an extension runtime.  The runtime may support one
     * or more clients withing the process.
     */
    @SuppressWarnings("CheckStyle")
    public static final class ConnectionID {
        /**
         * the process id for the connection
         */
        public final int connectionPID;
        /**
         * the connection identifier
         */
        public final int connectionID;

        // private, use valueOf
        public ConnectionID(final int connectionPID, final int connectionID) {
            this.connectionPID = connectionPID;
            this.connectionID = connectionID;
        }

        static ConnectionID valueOf(final int connectionPID, final int connectionID) {
            // for now return new, but this allows for caching
            return new ConnectionID(connectionPID, connectionID);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConnectionID)) return false;
            ConnectionID that = (ConnectionID) o;
            return
                    connectionPID == that.connectionPID &&
                            connectionID == that.connectionID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(connectionPID, connectionID);
        }

        @Override
        public String toString() {
            return "ConnectionID{" +
                    "connectionPID=" + connectionPID +
                    ", connectionID=" + connectionID +
                    '}';
        }
    }

    /**
     * A Connection to a service. A service may represent multiple Extensions, and an Extension may
     * have multiple clients (receivers).  Each client is give a unique routingID so that it may
     * be uniquely identified.
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    static class ServiceConnection extends L2_IRemoteService.Stub implements IMultiplexServiceConnection {

        /**
         * Handler for message management.
         */
        private final Handler mHandler;
        /**
         * The application specific receiver.
         */
        private ConnectionCallback mCallback;
        /**
         * Connected clients.
         */
        private final ConnectionMonitorList<L2_IRemoteClient, ConnectionID> mClients
                = new ConnectionMonitorList<L2_IRemoteClient, ConnectionID>() {

            @Override
            public void onDied(final L2_IRemoteClient client, final ConnectionID connectionID) {
                disconnectOnFailure(connectionID, ConnectionCallback.FAIL_CLIENT_DIED,
                        "Client died.");
            }
        };


        ServiceConnection(final Looper looper, @NonNull final ConnectionCallback callback,
                          final boolean async) {
            mCallback = callback;

            Handler handler = null;
            if (!async && null == looper) {
                // default use current threads looper
                handler = new Handler();
            } else if (!async) {
                handler = new Handler(looper);
            } else if (looper != null) {
                // Messages sent to an async handler are guaranteed to be ordered with respect
                // to one another, but not necessarily with respect to messages from other Handlers.</p >
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    handler = Handler.createAsync(looper);
                }
                // async on a specific looper is not supported before Pie
            }

            mHandler = handler;
        }


        /**
         * Notify receivers that the connection has failed.  This may be a result of IPC error
         * or the  rejecting the handshake.
         *
         * @param connectionID Client identifier.
         * @param errorCode    Cause of the error.
         * @param message      Human readable string
         */
        void disconnectOnFailure(final ConnectionID connectionID, final int errorCode,
                                 final String message) {
            Log.w(TAG, "Connection failure:" + errorCode + " " + message);

            if (null != mHandler) {
                mHandler.post(() -> notifyConnectionFailure(connectionID, errorCode, message));
            } else {
                notifyConnectionFailure(connectionID, errorCode, message);
            }
        }


        /**
         * Internal connection fail processing
         *
         * @param connectionID Client identifier.
         * @param errorCode    Cause of the error.
         * @param message      Human readable string
         */
        void notifyConnectionFailure(final ConnectionID connectionID, final int errorCode,
                                     final String message) {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onConnectionFailure(connectionID, errorCode, message);
            } else {
                // could have been disconnected
                Log.w(TAG, "Connection failure - extension no longer available.");
            }
        }


        /**
         * Confirm the transaction compatibility.  This method allows for support
         * of multiple versions.
         *
         * @param expectedVersion The server version desired by the client.
         * @return True if the server transaction version is supported.
         * @throws RemoteException on transaction failure.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized boolean L2_supportsTransactVersion(final int expectedVersion)
                throws RemoteException {
            return expectedVersion == TRANSACT_VERSION;
        }

        /**
         * A client is requesting an IPC connection after binding.  This initiates the connection
         * handshake by obtaining a client reference, and sending a service reference to the client.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_connect(final L2_IRemoteClient client, String configuration)
                throws RemoteException {
            if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, "L2_connect");

            if (null == client) {
                Log.e(TAG, "Client handshake failed, null client");
                disconnectOnFailure(null, ConnectionCallback.FAIL_HANDSHAKE,
                        "Null client connection.");
                return;
            }

            try {
                ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), client.L2_connectionID());
                Log.d(TAG, "L2_connect connectionID:" + connectionID);
                int transact = validate(client);
                // registerCallback the client, this adds a death monitor
                if (L2_IRemoteClient.TRANSACT_VERSION == transact
                        && mClients.register(client, connectionID)) {
                    // notify L3 the handshake is complete
                    if (null != mHandler) {
                        mHandler.post(() -> notifyOnConnect(connectionID, configuration));
                    } else {
                        notifyOnConnect(connectionID, configuration);
                    }
                    // send back success to finish the handshake.
                    client.L2_connectionAccept();
                } else {
                    disconnectOnFailure(connectionID, ConnectionCallback.FAIL_HANDSHAKE,
                            "Client - Server transaction incompatibility.");
                    client.L2_connectionReject(transact, "Service rejected.");
                }

            } catch (final RemoteException e) {
                // log only, assume connection is ok, client death will notify if there is an issue
                Log.e(TAG, "Client handshake failed", e);
            }

        }


        /**
         * Internal connection handling.
         *
         * @param connectionID  The unique connection identifier.
         * @param configuration Configuration for the connection.
         */

        private void notifyOnConnect(ConnectionID connectionID, String configuration) {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onConnect(connectionID, configuration);
            } else {
                // could have been disconnected
                Log.w(TAG, "Connection Closed - extension no longer available.");
            }
        }


        /**
         * Validates the handshake.  This method checks transaction version compatibility.
         * //TODO permissions, and auth token
         *
         * @param client The client.
         * @return TRANSACT_VERSION when successful, transact error code when unsuccessful.
         */
        @VisibleForTesting
        int validate(final L2_IRemoteClient client) {
            try {
                if (!client.L2_supportsTransactVersion(L2_IRemoteService.TRANSACT_VERSION)) {
                    return L2_IRemoteClient.TRANASCT_INCOMPATIBLE;
                }

            } catch (final RemoteException e) {
                // log only, assume connection is ok, client death will notify if there is an issue
                Log.e(TAG, "Client handshake failed", e);
            }

            return L2_IRemoteClient.TRANSACT_VERSION;
        }

        /**
         * IPC connection has been closed by the client.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_connectionClosed(final L2_IRemoteClient client, final String message)
                throws RemoteException {
            if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, "L2_connectionClosed: " + message);

            // unregister the client
            if (null == client) {
                Log.wtf(TAG, "Invalid client.");
                return;
            }

            try {
                mClients.unregister(client);
                ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), client.L2_connectionID());
                Log.d(TAG, "L2_connectionClosed connectionID:" + connectionID);
                if (null != mHandler) {
                    mHandler.post(() -> notifyConnectionClosed(connectionID, message));
                } else {
                    notifyConnectionClosed(connectionID, message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Client closed connection failure");
            }

        }


        /**
         * Internal. IPC connection has been closed by the client.
         */
        private void notifyConnectionClosed(final ConnectionID connectionID, String message) {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onConnectionClosed(connectionID, message);
            } else {
                // could have been disconnected
                Log.w(TAG, "Connection Closed - extension no longer available.");
            }
        }


        /**
         * Terminate the connection with all clients.
         *
         * @param message Readable close reason.
         */
        void closeConnection(final String message) {
            if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, "closeConnection: " + message);

            mCallback = null;
            // Broadcast to all clients
            final int N = mClients.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    L2_IRemoteClient client = mClients.getBroadcastItem(i);
                    client.L2_connectionClosed(message);
                    mClients.unregister(client);
                } catch (final RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                    Log.w(TAG, "Remote client close was unsuccessful");
                }
            }
            mClients.finishBroadcast();
        }


        /**
         * A client has sent a message.
         *
         * @param clientID  The client.
         * @param routingID The message sender.
         * @param message   The message.
         * @throws RemoteException Binder remote-invocation error.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_receive(final int clientID, final int routingID, final String message) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_receive connectionID:" + connectionID);

            if (null != mHandler) {
                mHandler.post(() -> notifyReceive(connectionID, routingID, message));
            } else {
                notifyReceive(connectionID, routingID, message);
            }
        }

        private void notifyResourceAvailable(final ConnectionID connectionID, final int routingID, Surface surface, Rect rect, String resourceID) throws IllegalStateException {
            if (null != mCallback && null != surface) {
                mCallback.onResourceAvailable(connectionID, routingID, surface, rect, resourceID);
            } else {
                Log.w(TAG, "Surface received - extension no longer available");
            }
        }

        private void notifyResourceUnavailable(final ConnectionID connectionID, final int routingID, String resourceID) {
            if (null != mCallback) {
                mCallback.onResourceUnavailable(connectionID, routingID, resourceID);
            } else {
                Log.w(TAG, "Surface unavailable - extension no longer available");
            }
        }

        /**
         * Internal receive processing.
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         * @param message      The message.
         */
        private void notifyReceive(final ConnectionID connectionID, final int routingID, final String message) throws IllegalStateException {

            if (null != mCallback && null != message) {
                // notify L3 of the message
                mCallback.onMessage(connectionID, routingID, message);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        /**
         * A client has lost focus.
         *
         * @param clientID  The client.
         * @param routingID The message sender.
         * @throws RemoteException Binder remote-invocation error.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_onFocusLost(int clientID, int routingID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_onFocusLost connectionID:" + connectionID);
            if (null != mHandler) {
                mHandler.post(() -> notifyFocusLost(connectionID, routingID));
            } else {
                notifyFocusLost(connectionID, routingID);
            }

        }

        /**
         * Internal focus lost processing.
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         */
        private void notifyFocusLost(final ConnectionID connectionID, final int routingID)
                throws IllegalStateException {

            // notify L3 of the message
            if (null != mCallback) {
                mCallback.onFocusLost(connectionID, routingID);
            } else {
                // could have been disconnected
                Log.w(TAG, "Focus lost - extension no longer available.");
            }
        }


        /**
         * A client has lost focus.
         *
         * @param clientID  The client.
         * @param routingID The sender.
         * @throws RemoteException Binder remote-invocation error.
         */
        @Override
        @SuppressWarnings("RedundantThrows")
        public synchronized void L2_onFocusGained(int clientID, int routingID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_onFocusGained connectionID:" + connectionID);
            if (null != mHandler) {
                mHandler.post(() -> notifyFocusGained(connectionID, routingID));
            } else {
                notifyFocusGained(connectionID, routingID);
            }
        }


        /**
         * Internal focus lost processing.
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         */
        private void notifyFocusGained(final ConnectionID connectionID, final int routingID)
                throws IllegalStateException {

            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onFocusGained(connectionID, routingID);
            } else {
                // could have been disconnected
                Log.w(TAG, "Focus gain - extension no longer available.");
            }
        }


        /**
         * A client has been paused
         *
         * @param clientID  The client.
         * @param routingID The message sender.
         * @throws RemoteException Binder remote-invocation error.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_onPause(int clientID, int routingID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_onPause connectionID:" + connectionID);
            if (null != mHandler) {
                mHandler.post(() -> notifyPause(connectionID, routingID));
            } else {
                notifyPause(connectionID, routingID);
            }

        }

        /**
         * Internal pause processing.
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         */
        private void notifyPause(final ConnectionID connectionID, final int routingID)
                throws IllegalStateException {

            // notify L3 of the message
            if (null != mCallback) {
                mCallback.onPause(connectionID, routingID);
            } else {
                // could have been disconnected
                Log.w(TAG, "Pause - extension no longer available.");
            }
        }


        /**
         * A client has resumed.
         *
         * @param clientID  The client.
         * @param routingID The sender.
         * @throws RemoteException Binder remote-invocation error.
         */
        @Override
        @SuppressWarnings("RedundantThrows")
        public synchronized void L2_onResume(int clientID, int routingID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_onResume connectionID:" + connectionID);
            if (null != mHandler) {
                mHandler.post(() -> notifyResume(connectionID, routingID));
            } else {
                notifyResume(connectionID, routingID);
            }
        }

        /**
         * Internal resume processing.
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         */
        private void notifyResume(final ConnectionID connectionID, final int routingID)
                throws IllegalStateException {

            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onResume(connectionID, routingID);
            } else {
                // could have been disconnected
                Log.w(TAG, "Resume - extension no longer available.");
            }
        }

        /**
         * A client has exited
         *
         * @param clientID  The client.
         * @param routingID The sender.
         * @throws RemoteException Binder remote invocation error.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_onExit(int clientID, int routingID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_onExit connectionID:" + connectionID);
            if (null != mHandler) {
                mHandler.post(() -> notifyOnExit(connectionID, routingID));
            } else {
                notifyFocusGained(connectionID, routingID);
            }
        }


        /**
         * Internal exit handler
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         */
        private void notifyOnExit(final ConnectionID connectionID, final int routingID)
                throws IllegalStateException {

            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onExit(connectionID, routingID);
            } else {
                // could have been disconnected
                Log.w(TAG, "On exit - extension no longer available.");
            }
        }


        /**
         * @return the number of connected clients.
         */
        @VisibleForTesting
        int getClientCount() {
            return mClients.getRegisteredCallbackCount();
        }


        /**
         * Send a message to a single clients.
         *
         * @param message The message
         * @throws RemoteException The transaction failed
         */
        @Override
        public synchronized void send(final ConnectionID connectionID, final int clientID,
                         final String message) throws RemoteException {

            if (null == connectionID || 0 == clientID)
                throw new RemoteException("No routing identifiers");

            L2_IRemoteClient client = mClients.get(connectionID);
            if (client == null) {
                throw new RemoteException("Client not found");
            }

            L2_send(client, clientID, message);
        }


        /**
         * Send a message to a single clients.
         *
         * @param message The message
         * @throws RemoteException The transaction failed
         */
        @Override
        public synchronized void L2_send(final L2_IRemoteClient client, final int routingID, final String message)
                throws RemoteException {
            if (BuildConfig.DEBUG_LOGGING) Log.d(TAG, "L2_send: " + message);

            client.L2_receive(routingID, message);

        }


        /**
         * Broadcast a message to all clients.
         *
         * @param message The message
         * @throws RemoteException The transaction failed
         */
        @Override
        public synchronized void sendBroadcast(final String message) throws RemoteException {

            if (null == message)
                throw new RemoteException("Empty broadcast message");

            L2_sendBroadcast(message);
        }

        /**
         * Broadcast a message to all clients.
         *
         * @param message The message.
         * @throws RemoteException The transaction failed
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_sendBroadcast(final String message) throws RemoteException {
            if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, "L2_send: " + message);

            final int N = mClients.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    mClients.getBroadcastItem(i).L2_receiveBroadcast(message);
                } catch (final RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                    Log.w(TAG, "Remote client close was unsuccessful");
                }
            }
            mClients.finishBroadcast();
        }

        /**
         * Send a message to a single clients.
         *
         * @param errorCode     The errorCode.
         * @param error         Readable message
         * @param failedMessage The failed payload.
         * @throws RemoteException The transaction failed
         */
        @Override
        public synchronized void sendFailure(final ConnectionID connectionID, final int clientID,
                                final int errorCode, final String error, final String failedMessage) throws RemoteException {

            if (null == connectionID || clientID == 0)
                throw new RemoteException("No routing identifiers");

            L2_IRemoteClient client = mClients.get(connectionID);
            if (client == null) {
                throw new RemoteException("Client not found");
            }

            L2_sendFailure(client, clientID, errorCode, error, failedMessage);
        }


        /**
         * Send a message to a single clients.
         *
         * @param routingID     The target client.
         * @param errorCode     The errorCode.
         * @param error         Readable message
         * @param failedMessage The failed payload.
         * @throws RemoteException The transaction failed
         */
        @Override
        public synchronized void L2_sendFailure(final L2_IRemoteClient client, final int routingID, final int errorCode, final String error,
                                   final String failedMessage) throws RemoteException {
            Log.d(TAG, "sendFailure: " + error);

            client.L2_messageFailure(routingID, errorCode, error, failedMessage);
        }

        public synchronized void requestResource(final ConnectionID connectionID, final int clientID,
                                    final String resourceId) throws RemoteException {
            if (null ==  connectionID || 0 == clientID) {
                throw new RemoteException("No routing identifiers");
            }

            L2_IRemoteClient client = mClients.get(connectionID);
            if (client == null) {
                throw new RemoteException("Client not found");
            }

            L2_requestResource(client, clientID, resourceId);
        }

        @Override
        public synchronized void L2_requestResource(L2_IRemoteClient client, int routingID, String resourceId) throws RemoteException {
            if (BuildConfig.DEBUG_LOGGING)  Log.d(TAG, "requestResource: " + resourceId);

            client.L2_onRequestResource(routingID, resourceId);
        }

        @Override
        public synchronized void L2_onResourceAvailable(int clientID, int routingID, Surface surface, Rect rect, String resourceID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, String.format("L2_onResourceAvailable connectionID:%s and resourceID:%s", connectionID, resourceID));
            if (null != mHandler) {
                mHandler.post(() -> notifyResourceAvailable(connectionID, routingID, surface, rect, resourceID));
            } else {
                notifyResourceAvailable(connectionID, routingID, surface, rect, resourceID);
            }
        }

        @Override
        public synchronized void L2_onResourceUnavailable(int clientID, int routingID, String resourceID) throws RemoteException {
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            Log.d(TAG, "L2_onResourceUnavailable connectionID:" + connectionID);
            if (null != mHandler) {
                mHandler.post(() -> notifyResourceUnavailable(connectionID, routingID, resourceID));
            } else {
                notifyResourceUnavailable(connectionID, routingID, resourceID);
            }
        }
    }


}


