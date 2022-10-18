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
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;

import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.ConnectionID;

/**
 * L2 Service.
 * This class is responsible for message events, connection request/refusals, and multiplexing.
 * May wrapL3 to facilitate communication. .
 */
public final class ExtensionMultiplexServiceV2 {
    /**
     * Debug tag.
     */
    private static final String TAG = "ExtensionMultiplexSvc";
    /**
     * Developer debugging.
     */
    private static final boolean DEBUG = false;
    /**
     * Singleton.
     */
    private static final ExtensionMultiplexServiceV2 sInstance = new ExtensionMultiplexServiceV2();
    /**
     * Connection monitor, monitors death of client connections and notifies the runtime of failures.
     */
    private final ConnectionMonitorList<ServiceConnection, Object> mConnections =
            new ConnectionMonitorList<ServiceConnection, Object>() {

                // A client connection has died and has been removed from this list.
                // Note: "Callback" here is the death monitor, and implemented by ServiceConnection
                @Override
                public void onCallbackDied(final ServiceConnection connection, final Object cookie) {
                    if (DEBUG) Log.d(TAG, "Service has died: " + cookie);

                    if (connection != null) {
                        // notify connection callbacks that the server died
                        connection.disconnectOnFailure(null,
                                ConnectionCallback.FAIL_SERVER_DIED, "The service has died");
                    }
                }
            };

    private ExtensionMultiplexServiceV2() {
        // private constructor for singleton
        super();
    }

    /**
     * @return Single instance.
     */
    public static ExtensionMultiplexServiceV2 getInstance() {
        return ExtensionMultiplexServiceV2.sInstance;
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
        if (DEBUG) Log.d(TAG, message);

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

    interface ConnectionCallback {
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
        void onConnect(final ConnectionID connectionID, final String configuration);

        /**
         * The IPC connection handshake to the service has been closed by the service.
         *
         * @param connectionID The unique ID of the client.
         * @param message      Readable message.
         */
        void onConnectionClosed(final ConnectionID connectionID, final String message);

        /**
         * A connection has failed.  This is caused by death of the server or client.
         *
         * @param connectionID The connectionID from {@link #onConnect(ConnectionID, String)}, null if the
         *                     client could not be identified.
         * @param error        Reason for the failure.
         * @param message      Readable message.
         */
        void onConnectionFailure(final ConnectionID connectionID, final int error, final String message);

        /**
         * A message has been sent by a client.
         *
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param message      The message.
         */
        void onMessage(
                final ConnectionID connectionID,
                final int clientID,
                final ActivityDescriptor activity,
                final String message);

        /**
         * A resource is available for an Extension Component. This is usually in response to a
         * ResourceRequest made by the service to the client.
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param surface      Resource/Surface associated with unique resourceID.
         * @param rect         Frame dimensions of the resource.
         * @param resourceID   Unique Resource ID.
         */
        void onResourceAvailable(
                final ConnectionID connectionID,
                final int clientID,
                final ActivityDescriptor activity,
                final Surface surface,
                final Rect rect,
                final String resourceID);

        /**
         * A resource is not available for an Extension Component. This is usually in response to a
         * ResourceRequest made by the service to the client. In this case, the request failed.
         * @param connectionID The unique connection identifier.
         * @param clientID     The unique client identifier.
         * @param resourceID   Unique Resource ID.
         */
        void onResourceUnavailable(
                final ConnectionID connectionID,
                final int clientID,
                final ActivityDescriptor activity,
                final String resourceID);

        /**
         * @see com.amazon.alexaext.IExtension
         */
        void onRegistered(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity);
        void onUnregistered(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity);
        void onSessionStarted(final ConnectionID connectionID, final int clientID, final SessionDescriptor session);
        void onSessionEnded(final ConnectionID connectionID, final int clientID, final SessionDescriptor session);
        void onForeground(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity);
        void onBackground(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity);
        void onHidden(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity);
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
        void send(ConnectionID connectionID, int clientID, ActivityDescriptor activity, String message) throws RemoteException;

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
        void sendFailure(ConnectionID connectionID, int clientID, ActivityDescriptor activity, int errorCode, String error,
                         String failedMessage) throws RemoteException;

    }

    /**
     * A Connection to a service. A service may represent multiple Extensions, and an Extension may
     * have multiple clients (receivers).  Each client is give a unique routingID so that it may
     * be uniquely identified.
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    static class ServiceConnection extends L2_IRemoteServiceV2.Stub implements IMultiplexServiceConnection {

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
            if (DEBUG) Log.i(TAG, "L2_connect");

            if (null == client) {
                Log.e(TAG, "Client handshake failed, null client");
                disconnectOnFailure(null, ConnectionCallback.FAIL_HANDSHAKE,
                        "Null client connection.");
                return;
            }

            try {
                ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), client.L2_connectionID());
                // registerCallback the client, this adds a death monitor
                if (validate(client) && mClients.register(client, connectionID)) {
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
                    client.L2_connectionReject(L2_IRemoteClient.TRANASCT_INCOMPATIBLE, "Service rejected.");
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
        boolean validate(final L2_IRemoteClient client) {
            try {
                if (!client.L2_supportsTransactVersion(L2_IRemoteServiceV2.TRANSACT_VERSION)) {
                    return false;
                }
            } catch (final RemoteException e) {
                // log only, assume connection is ok, client death will notify if there is an issue
                Log.e(TAG, "Client handshake failed", e);
            }

            return true;
        }

        /**
         * IPC connection has been closed by the client.
         */
        @SuppressWarnings("RedundantThrows")
        @Override
        public synchronized void L2_connectionClosed(final L2_IRemoteClient client, final String message)
                throws RemoteException {
            if (DEBUG) Log.i(TAG, "L2_connectionClosed: " + message);

            // unregister the client
            if (null == client) {
                Log.wtf(TAG, "Invalid client.");
                return;
            }

            try {
                mClients.unregister(client);
                ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), client.L2_connectionID());
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
            if (DEBUG) Log.i(TAG, "closeConnection: " + message);

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
        public synchronized void L2_receive(final int clientID, final int routingID, final ActivityDescriptor activity, final String message) throws RemoteException {
            if (DEBUG) Log.i(TAG, "L2_receive: " + message);

            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);

            if (null != mHandler) {
                mHandler.post(() -> notifyReceive(connectionID, routingID, activity, message));
            } else {
                notifyReceive(connectionID, routingID, activity, message);
            }
        }

        /**
         * Internal receive processing.
         *
         * @param connectionID The connection message was received on.
         * @param routingID    The sender.
         * @param message      The message.
         */
        private void notifyReceive(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity, final String message) throws IllegalStateException {

            if (null != mCallback && null != message) {
                // notify L3 of the message
                mCallback.onMessage(connectionID, routingID, activity, message);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
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
        public synchronized void send(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity,
                         final String message) throws RemoteException {

            if (null == connectionID || 0 == clientID)
                throw new RemoteException("No routing identifiers");

            L2_IRemoteClient client = mClients.get(connectionID);
            if (client == null) {
                throw new RemoteException("Client not found");
            }

            L2_send(client, clientID, activity, message);
        }


        /**
         * Send a message to a single clients.
         *
         * @param message The message
         * @throws RemoteException The transaction failed
         */
        @Override
        public synchronized void L2_send(final L2_IRemoteClient client, final int routingID, final ActivityDescriptor activity, final String message)
                throws RemoteException {
            if (DEBUG) Log.i(TAG, "L2_send: " + message);

            client.L2_receiveV2(routingID, activity, message);

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
        public synchronized void sendFailure(final ConnectionID connectionID, final int clientID, final ActivityDescriptor activity,
                                final int errorCode, final String error, final String failedMessage) throws RemoteException {

            if (null == connectionID || clientID == 0)
                throw new RemoteException("No routing identifiers");

            L2_IRemoteClient client = mClients.get(connectionID);
            if (client == null) {
                throw new RemoteException("Client not found");
            }

            L2_sendFailure(client, clientID, activity, errorCode, error, failedMessage);
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
        public synchronized void L2_sendFailure(final L2_IRemoteClient client, final int routingID, final ActivityDescriptor activity, final int errorCode, final String error,
                                   final String failedMessage) throws RemoteException {
            if (DEBUG) Log.i(TAG, "sendFailure: " + error);

            client.L2_messageFailureV2(routingID, activity, errorCode, error, failedMessage);
        }

        @Override
        public synchronized void L2_onResourceAvailable(int clientID, int routingID, final ActivityDescriptor activity, Surface surface, Rect rect, String resourceID) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onResourceAvailable: " + resourceID);
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyResourceAvailable(connectionID, routingID, activity, surface, rect, resourceID));
            } else {
                notifyResourceAvailable(connectionID, routingID, activity, surface, rect, resourceID);
            }
        }

        private void notifyResourceAvailable(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity, Surface surface, Rect rect, String resourceID) throws IllegalStateException {
            if (null != mCallback && null != surface) {
                mCallback.onResourceAvailable(connectionID, routingID, activity, surface, rect, resourceID);
            } else {
                Log.w(TAG, "Surface received - extension no longer available");
            }
        }

        @Override
        public synchronized void L2_onResourceUnavailable(int clientID, int routingID, final ActivityDescriptor activity, String resourceID) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onResourceUnavailable: " + resourceID);
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyResourceUnavailable(connectionID, routingID, activity, resourceID));
            } else {
                notifyResourceUnavailable(connectionID, routingID, activity, resourceID);
            }
        }

        private void notifyResourceUnavailable(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity, String resourceID) {
            if (null != mCallback) {
                mCallback.onResourceUnavailable(connectionID, routingID, activity, resourceID);
            } else {
                Log.w(TAG, "Surface unavailable - extension no longer available");
            }
        }

        @Override
        public void L2_onRegistered(int clientID, int routingID, ActivityDescriptor activity) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onRegistered: " + activity.getActivityId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyRegistered(connectionID, routingID, activity));
            } else {
                notifyRegistered(connectionID, routingID, activity);
            }
        }

        private void notifyRegistered(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onRegistered(connectionID, routingID, activity);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        @Override
        public void L2_onUnregistered(int clientID, int routingID, ActivityDescriptor activity) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onUnregistered: " + activity.getActivityId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyUnregistered(connectionID, routingID, activity));
            } else {
                notifyUnregistered(connectionID, routingID, activity);
            }
        }

        private void notifyUnregistered(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onUnregistered(connectionID, routingID, activity);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        @Override
        public void L2_onSessionStarted(int clientID, int routingID, SessionDescriptor session) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onSessionStarted: " + session.getId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifySessionStarted(connectionID, routingID, session));
            } else {
                notifySessionStarted(connectionID, routingID, session);
            }
        }

        private void notifySessionStarted(final ConnectionID connectionID, final int routingID, final SessionDescriptor session) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onSessionStarted(connectionID, routingID, session);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        @Override
        public void L2_onSessionEnded(int clientID, int routingID, SessionDescriptor session) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onSessionEnded: " + session.getId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifySessionEnded(connectionID, routingID, session));
            } else {
                notifySessionEnded(connectionID, routingID, session);
            }
        }

        private void notifySessionEnded(final ConnectionID connectionID, final int routingID, final SessionDescriptor session) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onSessionEnded(connectionID, routingID, session);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        @Override
        public void L2_onForeground(int clientID, int routingID, ActivityDescriptor activity) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onForeground: " + activity.getActivityId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyForeground(connectionID, routingID, activity));
            } else {
                notifyForeground(connectionID, routingID, activity);
            }
        }

        private void notifyForeground(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onForeground(connectionID, routingID, activity);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        @Override
        public void L2_onBackground(int clientID, int routingID, ActivityDescriptor activity) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onBackground: " + activity.getActivityId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyBackground(connectionID, routingID, activity));
            } else {
                notifyBackground(connectionID, routingID, activity);
            }
        }

        private void notifyBackground(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onBackground(connectionID, routingID, activity);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }

        @Override
        public void L2_onHidden(int clientID, int routingID, ActivityDescriptor activity) throws RemoteException {
            if (DEBUG) Log.i(TAG, "onHidden: " + activity.getActivityId());
            ConnectionID connectionID = ConnectionID.valueOf(getCallingPid(), clientID);
            if (null != mHandler) {
                mHandler.post(() -> notifyHidden(connectionID, routingID, activity));
            } else {
                notifyHidden(connectionID, routingID, activity);
            }
        }

        private void notifyHidden(final ConnectionID connectionID, final int routingID, final ActivityDescriptor activity) throws IllegalStateException {
            if (null != mCallback) {
                // notify L3 of the message
                mCallback.onHidden(connectionID, routingID, activity);
            } else {
                // could have been disconnected
                Log.w(TAG, "Message received - extension no longer available.");
            }
        }
    }
}


