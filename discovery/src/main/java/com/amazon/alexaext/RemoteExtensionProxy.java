/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        mProxyDelegate.setOnInternalMessageAction((activity, message, registered) -> {
            try {
                final JSONObject reader = new JSONObject(message);
                final String method = reader.optString("method", "");
                if (!(method.equals(METHOD_REGISTER_SUCCESS) || method.equals(METHOD_REGISTER_FAILURE)) && !registered) {
                    return false;
                }

                switch (method) {
                    case METHOD_EVENT:
                        invokeExtensionEventHandler(activity, message);
                        break;
                    case METHOD_LIVE_DATA_UPDATE:
                        invokeLiveDataUpdate(activity, message);
                        break;
                    case METHOD_COMMAND_SUCCESS:
                    case METHOD_COMMAND_FAILURE:
                        commandResult(activity, message);
                        break;
                    case METHOD_REGISTER_SUCCESS:
                    case METHOD_REGISTER_FAILURE:
                        registrationResult(activity, message);
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
    protected boolean initialize(@NonNull final String uri) {
        return mProxyDelegate.onProxyInitialize(uri);
    }

    @Override
    protected boolean invokeCommand(@NonNull final ActivityDescriptor activity, final String command) {
        return mProxyDelegate.sendMessage(activity, command);
    }

    @Override
    protected boolean sendMessage(@NonNull final ActivityDescriptor activity, final String message) {
        return mProxyDelegate.sendMessage(activity, message);
    }

    @Override
    protected boolean requestRegistration(@NonNull final ActivityDescriptor activity, final String request) {
        return mProxyDelegate.onRequestRegistration(activity, request);
    }

    @Override
    protected void onRegistered(@NonNull final ActivityDescriptor activity) {
        mProxyDelegate.onRegisteredInternal(activity);
    }

    @Override
    protected void onUnregistered(@NonNull final ActivityDescriptor activity) {
        mProxyDelegate.onUnregisteredInternal(activity);
    }

    @Override
    //TODO: raw use of parameterized generic, this code smells
    protected void onResourceReady(@NonNull final ActivityDescriptor activity, final ResourceHolder resourceHolder) {
       mProxyDelegate.onResourceReadyInternal(activity, resourceHolder);
    }

    @Override
    protected void onSessionStarted(SessionDescriptor session) {
        mProxyDelegate.onSessionStartedInternal(session);
    }

    @Override
    protected void onSessionEnded(SessionDescriptor session) {
        mProxyDelegate.onSessionEndedInternal(session);
    }

    @Override
    protected void onForeground(ActivityDescriptor activity) {
        mProxyDelegate.onForegroundInternal(activity);
    }

    @Override
    protected void onBackground(ActivityDescriptor activity) {
        mProxyDelegate.onBackgroundInternal(activity);
    }

    @Override
    protected void onHidden(ActivityDescriptor activity) {
        mProxyDelegate.onHiddenInternal(activity);
    }
}
