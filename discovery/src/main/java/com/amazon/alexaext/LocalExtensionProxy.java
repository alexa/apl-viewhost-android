/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Proxy for local (built-in) extensions.
 */
public class LocalExtensionProxy extends ExtensionProxy {
    private IExtension mExtension;

    public LocalExtensionProxy(IExtension extension) {
        super(extension.getURIs().iterator().next());
        mExtension = extension;
    }

    @Override
    protected boolean initializeInternal(String uri) {
        mExtension.registerEventCallback(this::invokeExtensionEventHandler);
        mExtension.registerLiveDataUpdateCallback(this::invokeLiveDataUpdate);
        mExtension.registerCommandResultCallback(this::commandResult);
        return true;
    }

    @Override
    protected boolean invokeCommandInternal(String uri, String command) {
        int id;
        try {
            JSONObject reader = new JSONObject(command);
            id = reader.getInt("id");
        } catch (JSONException e) {
            return false;
        }

        return mExtension.onCommand(id, uri, command);
    }

    @Override
    protected boolean sendMessageInternal(String uri, String message) {
        return mExtension.onMessage(uri, message);
    }

    @Override
    protected void onResourceReadyInternal(String extensionURI, ResourceHolder resourceHolder) {
        mExtension.onResourceReady(extensionURI, resourceHolder);
    }

    @Override
    protected boolean requestRegistrationInternal(String uri, String request) {
        if (!mExtension.getURIs().contains(uri)) {
            return false;
        }

        String registerMessage = mExtension.createRegistration(uri, request);
        registrationResult(uri, registerMessage);
        return true;
    }

    @Override
    protected void onRegisteredInternal(String uri, String token) {
        mExtension.onRegistered(uri, token);
    }

    @Override
    protected void onUnregisteredInternal(String uri, String token) {
        mExtension.onUnregistered(uri, token);
    }
}
