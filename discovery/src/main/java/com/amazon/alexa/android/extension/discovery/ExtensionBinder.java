/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexa.android.extension.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.util.Map;

/**
 * L1 Client/Service Extension IPC.
 * This class provides the platform specific server binding, and connection management.
 */
final class ExtensionBinder {

    private static final String TAG = "ExtensionBinder";
    private static final boolean DEBUG = false;

    // Open bindings
    private final Map<String, ExtensionConnection> mBindings = new ArrayMap<>();
    // Lock for updates to connections
    private final Object mConnectionLock = new Object();


    /**
     * Callback interface for lifecycle of L1 binding connection.  A service may support multiple
     * extensions.
     */
    interface ConnectionCallback {

        /**
         * An Extension service has been discovered and two-way communication is established.
         * ServiceConnections persist after binding failures, it is the responsibility of the client
         * to call {@link ExtensionBinder#unbind(Context, String)}
         *
         * @param service Interface to the extension instance.
         */
        void bindingSuccess(L2_IRemoteService service);

        /**
         * Called when a connection to the Extension service has been lost due to unexpected process
         * termination (crash). The connection stays open and {@link #bindingSuccess(L2_IRemoteService)}
         * is called whe the connection is reestablished.
         **/
        int FAIL_DISCONNECTED = -100;

        /**
         * Called whe the connection to the Extension service has died and cannot be restored due to
         * the service being missing (removed from device).
         * {@link ExtensionBinder#bind(Context, String, ConnectionCallback)} should be retried.
         */
        int FAIL_DIED = -200;

        /**
         * Called whe the connection to the Extension service is established but the service has
         * not allowed 2-way communication and is therefore unusable.  This is a problem with
         * the service that may or may not be recoverable by retrying with
         * {@link ExtensionBinder#bind(Context, String, ConnectionCallback)}.
         */
        int FAIL_COM = -300;

        /**
         * A connection to an Extension service has failed.  Some failures auto-recover, some do not.
         * ServiceConnections persist after binding failures, it is the responsibility of the client
         * to call {@link ExtensionBinder#unbind(Context, String)}
         * See {@link  #FAIL_DISCONNECTED}
         * See {@link #FAIL_COM}
         * See {@link #FAIL_COM}
         *
         * @param errorCode Error identifier.
         * @param error     Human readable message.
         */
        void bindingFailure(int errorCode, String error);

    }


    /**
     * Request to connect to an extension service.  This method returns {@code true} if an extension
     * connection is established and two way communication is initiated.
     * {@link ConnectionCallback#bindingSuccess(L2_IRemoteService)} is called when two way
     * communication is in place. {@link ConnectionCallback#bindingFailure(int, String)}
     * is called if the binding fails;  No callback is made if the method returns {@code false} as
     * a result of an inability to attempt the connection.
     * <p>
     * Binding supports a single callback per connection, callers
     * should see {@link ExtensionMultiplexClient} to manage multiplexing.
     *
     * @param context      Android Context.
     * @param extensionURI The extension URI or alias.
     * @param callback     callback for message handling.
     * @return {@code true} if the system is in the process of bringing up a
     * service connection. {@code false} if the system couldn't find the service, or if the client
     * doesn't have permission to connect to it.
     * @throws IllegalStateException if the binding already exists.
     */
    boolean bind(@NonNull final Context context, @NonNull final String extensionURI,
                 @NonNull final ConnectionCallback callback) throws IllegalStateException {

        //noinspection ConstantConditions
        if (null == context) {
            Log.e(TAG, "Null context");
            return false;
        }

        if (TextUtils.isEmpty(extensionURI)) {
            Log.e(TAG, "Invalid extension URI:" + extensionURI);
            return false;
        }

        //noinspection ConstantConditions
        if (null == callback) {
            Log.e(TAG, "Null callback");
            return false;
        }

        boolean result;

        synchronized (mConnectionLock) {

            // check for existing connection
            ExtensionConnection connection = mBindings.get(extensionURI);
            if (null != connection) {
                // connection already exists
                Log.wtf(TAG, "Expected L2 Multiplex client to manage duplicate requests");
                throw new IllegalStateException("Multiple requests on single extensionURI is invalid");
            }

            // Create a connection callback and Intent for discovery
            final Intent intent = ExtensionDiscovery.createExtensionIntent();
            connection = new ExtensionConnection(callback);

            // discover the extension package
            final ComponentName svcComponent = ExtensionDiscovery.getInstance(context).getComponentName(extensionURI);

            if (null == svcComponent) {
                Log.e(TAG, "Cannot find service component: " + extensionURI);
                return false;
            }

            // launch the service
            intent.setComponent(svcComponent);
            try {
                result = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (result) {
                    mBindings.put(extensionURI, connection);
                } else {
                    Log.e(TAG, "Android binding failed to connect:" + svcComponent);
                }
            } catch (final SecurityException e) {
                Log.e(TAG, "Permission missing, the runtime does not have sufficient"
                        + " permissions for service:" + svcComponent, e);
                // the caller does not have permission to access the service or the service
                // can not be found.
                return false;
            }

        }
        return result;
    }


    /**
     * Close an Extension connection.  The connection may be closed using any extensionURI
     * used in {@link #bind(Context, String, ConnectionCallback)}, meaning a service supporting
     * multiple extensions will return {@code false} on secondary calls to this method.
     *
     * @param context      Android context.
     * @param extensionURI The extension URI or alias. Matches {@link #bind(Context, String, ConnectionCallback)}
     * @return {@code true} if the service connection existed; {@code false} otherwise.
     */
    boolean unbind(@NonNull final Context context, @NonNull final String extensionURI) {
        if (DEBUG) Log.d(TAG, "unbind: " + extensionURI);

        boolean result = false;
        ServiceConnection connection;
        synchronized (mConnectionLock) {
            connection = mBindings.remove(extensionURI);
            if (null != connection) {
                context.unbindService(connection);
                result = true;
            }
        }
        return result;
    }

    /**
     * Unbind all connections.
     *
     * @param context Android Context.
     */
    void unbindAll(@NonNull final Context context) {
        synchronized (mConnectionLock) {
            for (String extensionURI : mBindings.keySet()) {
                try {
                    unbind(context, extensionURI);
                } catch (Exception e) {
                    Log.wtf(TAG, "Unbind Failure " + extensionURI);
                }
            }
        }
    }

    /**
     * @return the number of open connections.
     */
    @VisibleForTesting
    int getBindingCount() {
        synchronized (mConnectionLock) {
            return mBindings.size();
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService().
     * Updates the connections collection in the binding singleton.
     */
    static class ExtensionConnection implements ServiceConnection {

        private final ConnectionCallback mCallback;

        ExtensionConnection(@NonNull final ConnectionCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName componentName, final IBinder service) {
            if (DEBUG) Log.d(TAG, "onServiceConnected: " + componentName);

            // Register two way communication with L2 callback
            L2_IRemoteService svc = L2_IRemoteService.Stub.asInterface(service);
            mCallback.bindingSuccess(svc);
        }

        @Override
        public void onServiceDisconnected(final ComponentName componentName) {
            if (DEBUG) Log.d(TAG, "onServiceDisconnected: " + componentName);

            mCallback.bindingFailure(ConnectionCallback.FAIL_DISCONNECTED,
                    "binding terminated");
        }

        @Override
        public void onBindingDied(final ComponentName componentName) {
            if (DEBUG) Log.d(TAG, "onBindingDied: " + componentName);
            mCallback.bindingFailure(ConnectionCallback.FAIL_DIED, "binding died");
        }


        @Override
        public void onNullBinding(final ComponentName componentName) {
            if (DEBUG) Log.d(TAG, "onNullBinding: " + componentName);
            mCallback.bindingFailure(ConnectionCallback.FAIL_COM, "null binding");
        }

    }

}
