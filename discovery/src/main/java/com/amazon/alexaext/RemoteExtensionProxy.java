/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteExtensionProxy extends ExtensionProxy  {
    private static final String TAG = "MultiplexExtensionProxy";

    private static final String METHOD_EVENT = "Event";
    private static final String METHOD_LIVE_DATA_UPDATE = "LiveDataUpdate";
    private static final String METHOD_COMMAND_SUCCESS = "CommandSuccess";
    private static final String METHOD_COMMAND_FAILURE = "CommandFailure";
    private static final String METHOD_REGISTER_SUCCESS = "RegisterSuccess";
    private static final String METHOD_REGISTER_FAILURE = "RegisterFailure";

    @NonNull
    private final BaseRemoteProxyDelegate mProxyDelegate;


    public RemoteExtensionProxy(@NonNull final String uri, @NonNull final BaseRemoteProxyDelegate proxyDelegate) {
        super(uri);

        mProxyDelegate = proxyDelegate;
        mProxyDelegate.setOnInternalMessageAction((extensionUri, message, registered) -> {
            try {
                final JSONObject reader = new JSONObject(message);
                final String method = reader.optString("method", "");
                if (!(method.equals(METHOD_REGISTER_SUCCESS) || method.equals(METHOD_REGISTER_FAILURE)) && !registered) {
                    return false;
                }

                switch (method) {
                    case METHOD_EVENT:
                        invokeExtensionEventHandler(extensionUri, message);
                        break;
                    case METHOD_LIVE_DATA_UPDATE:
                        invokeLiveDataUpdate(extensionUri, message);
                        break;
                    case METHOD_COMMAND_SUCCESS:
                    case METHOD_COMMAND_FAILURE:
                        commandResult(extensionUri, message);
                        break;
                    case METHOD_REGISTER_SUCCESS:
                    case METHOD_REGISTER_FAILURE:
                        registrationResult(extensionUri, message);
                        break;
                }
            } catch (JSONException e) {
                Log.w(TAG, "Message processing failure.");
            }
            return true;
        });
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mProxyDelegate.disconnect(getUri(), "Clean up.");
    }

    @Override
    protected boolean initializeInternal(@NonNull final String uri) {
        return mProxyDelegate.onProxyInitialize(uri);
    }

    @Override
    protected boolean invokeCommandInternal(@NonNull final String uri, final String command) {
        return mProxyDelegate.sendMessage(uri, command);
    }

    @Override
    protected boolean sendMessageInternal(@NonNull final String uri, final String message) {
        return mProxyDelegate.sendMessage(uri, message);
    }

    @Override
    protected boolean requestRegistrationInternal(@NonNull final String uri, final String request) {
        return mProxyDelegate.onRequestRegistration(uri, request);
    }

    @Override
    protected void onRegisteredInternal(@NonNull final String uri, final String token) {
        mProxyDelegate.onRegisteredInternal(uri);
    }

    @Override
    protected void onUnregisteredInternal(@NonNull final String uri, final String token) {
        // Now we can transfer other messages.
       mProxyDelegate.onUnregisteredInternal(uri);
    }

    @Override
    //TODO: raw use of parameterized generic, this code smells
    protected void onResourceReadyInternal(@NonNull final String extensionURI, final ResourceHolder resourceHolder) {
       mProxyDelegate.onResourceReadyInternal(resourceHolder);
    }

    /**
     * Invoked when controlling instance gained platform focus.
     */
    @Deprecated
    public void onFocusGained() {
        mProxyDelegate.onFocusGained(getUri());
    }

    /**
     * Invoked when controlling instance lost platform focus.
     */
    @Deprecated
    public void onFocusLost() {
        mProxyDelegate.onFocusLost();
    }
}
