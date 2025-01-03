/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Proxy for local (built-in) extensions.
 */
public class LocalExtensionProxy extends ExtensionProxy {
    private final IExtension mExtension;

    /**
     * Backwards compatibility.
     */
    @Nullable
    @Deprecated
    private ActivityDescriptor mCachedDescriptor;

    public LocalExtensionProxy(IExtension extension) {
        super(extension.getURIs().iterator().next());
        mExtension = extension;
    }

    @Override
    protected boolean initialize(String uri) {
        mExtension.registerEventCallback((IExtension.IExtensionActivityEventCallback) this::invokeExtensionEventHandler);
        mExtension.registerLiveDataUpdateCallback((IExtension.ILiveDataActivityUpdateCallback) this::invokeLiveDataUpdate);
        mExtension.registerCommandResultCallback((IExtension.IExtensionActivityCommandResultCallback) this::commandResult);

        // Backwards compatibility
        mExtension.registerEventCallback((IExtension.IExtensionEventCallback) (eUri, event) -> invokeExtensionEventHandler(mCachedDescriptor, event));
        mExtension.registerLiveDataUpdateCallback((ILiveDataUpdateCallback) (eUri, event) -> invokeLiveDataUpdate(mCachedDescriptor, event));
        mExtension.registerCommandResultCallback((IExtension.IExtensionCommandResultCallback) (eUri, event) -> commandResult(mCachedDescriptor, event));
        return true;
    }

    @Override
    protected boolean invokeCommand(ActivityDescriptor activity, String command) {
        int id;
        try {
            JSONObject reader = new JSONObject(command);
            id = reader.getInt("id");
        } catch (JSONException e) {
            return false;
        }

        return mExtension.onCommand(activity, id, command);
    }

    @Override
    protected boolean sendMessage(ActivityDescriptor activity, String message) {
        return mExtension.onMessage(activity, message);
    }

    @Override
    protected void onResourceReady(ActivityDescriptor activity, ResourceHolder resourceHolder) {
        mExtension.onResourceReady(activity, resourceHolder);
    }

    @Override
    protected boolean requestRegistration(ActivityDescriptor activity, String request) {
        mCachedDescriptor = activity;
        if (!mExtension.getURIs().contains(activity.getURI())) {
            return false;
        }

        String registerMessage = mExtension.createRegistration(activity, request);
        registrationResult(activity, registerMessage);
        return true;
    }

    @Override
    protected void onRegistered(ActivityDescriptor activity) {
        mExtension.onRegistered(activity);
    }

    @Override
    protected void onUnregistered(ActivityDescriptor activity) {
        mCachedDescriptor = null;
        mExtension.onUnregistered(activity);
    }

    @Override
    protected void onSessionStarted(SessionDescriptor session) {
        mExtension.onSessionStarted(session);
    }

    @Override
    protected void onSessionEnded(SessionDescriptor session) {
        mExtension.onSessionEnded(session);
    }

    @Override
    protected void onForeground(ActivityDescriptor activity) {
        mExtension.onForeground(activity);
    }

    @Override
    protected void onBackground(ActivityDescriptor activity) {
        mExtension.onBackground(activity);
    }

    @Override
    protected void onHidden(ActivityDescriptor activity) {
        mExtension.onHidden(activity);
    }
}