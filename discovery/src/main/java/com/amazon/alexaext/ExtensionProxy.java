/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amazon.common.BoundObject;

/**
 * ExtensionProxy wrapper. Exposes an ability to define extensions that could be handled by internal
 * core systems.
 */
public abstract class ExtensionProxy extends BoundObject {

    public ExtensionProxy(String uri) {
        final long handle = nCreate(uri);
        bind(handle);
    }

    /**
     * Process request to initialize an extension. To be overriden by specific implementation.
     *
     * @param uri Extension URI.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean initializeInternal(String uri);

    /**
     * Process request to invoke a command. To be overriden by specific implementation.
     * Result needs to be returned by calling @see ExtensionProxy::commandResult with appropriate parameters.
     *
     * @param uri Extension URI.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean invokeCommandInternal(String uri, String command);

    /**
     * Process request to pass a message. To be overriden by specific implementation.
     * No response is expected.
     *
     * @param uri Extension URI.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean sendMessageInternal(String uri, String command);

    /**
     * Process registration request. To be overriden by specific implementation.
     * Result needs to be returned by calling @see ExtensionProxy::registrationResult with appropriate parameters.
     *
     * @param uri Extension URI.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean requestRegistrationInternal(String uri, String request);

    /**
     * Called when extension was properly registered.
     *
     * @param uri   extension URI.
     * @param token registration token.
     */
    protected abstract void onRegisteredInternal(String uri, String token);

    /**
     * Invoked when an extension is unregistered. Session represented by the provided token is no longer valid.
     *
     * @param uri   extension URI.
     * @param token registration token.
     */
    protected void onUnregisteredInternal(String uri, String token) {}


    /**
     * Invoked when a system rendering resource, such as display surface, is ready for use. This method
     * will be called after the viewhost receives a "Component" message with a resource state of
     * "Ready".  Not all execution environments support shared rendering resources.
     *
     * @param extensionURI   The extension URI.
     * @param resourceHolder Access to the rendering resource.
     */
    protected void onResourceReadyInternal(String extensionURI, ResourceHolder resourceHolder) {
        // default to no extension support
    }

    @NonNull
    @SuppressWarnings("unused")
    @VisibleForTesting
    public boolean initialize(String uri) {
        return initializeInternal(uri);
    }

    @NonNull
    @SuppressWarnings("unused")
    @VisibleForTesting
    public boolean requestRegistration(String uri, String request) {
        return requestRegistrationInternal(uri, request);
    }

    /**
     * Pass registration result to the core.
     *
     * @param uri    extension URI.
     * @param result result message.
     */
    public void registrationResult(String uri, String result) {
        nRegistrationResult(getNativeHandle(), uri, result);
    }

    @NonNull
    @SuppressWarnings("unused")
    private boolean invokeCommand(String uri, String command) {
        return invokeCommandInternal(uri, command);
    }

    @NonNull
    @SuppressWarnings("unused")
    private boolean sendMessage(String uri, String command) {
        return sendMessageInternal(uri, command);
    }

    /**
     * Pass registration result to the core.
     *
     * @param uri    extension URI.
     * @param result result message.
     */
    public void commandResult(String uri, String result) {
        nCommandResult(getNativeHandle(), uri, result);
    }

    @NonNull
    @SuppressWarnings("unused")
    private void onRegistered(String uri, String token) {
        onRegisteredInternal(uri, token);
    }

    @NonNull
    @SuppressWarnings("unused")
    private void onUnregistered(String uri, String token) {
        onUnregisteredInternal(uri, token);
    }

    @SuppressWarnings("unused")
    @Deprecated
    public void onRequestResource(String extensionURI, String resourceId) {
        // Deprecated the extension is notified without request
    }

    @SuppressWarnings("unused")
    void onResourceReady(String extensionURI, ResourceHolder resourceHolder) {
        onResourceReadyInternal(extensionURI, resourceHolder);
    }

    /**
     * @return extension URI.
     */
    public String getUri() {
        return nGetUri(getNativeHandle());
    }

    /**
     * Invoke extension event handler.
     *
     * @param uri   extension URI.
     * @param event event message.
     * @return true if succeeded, false otherwise.
     */
    public boolean invokeExtensionEventHandler(String uri, String event) {
        return nInvokeExtensionEventHandler(getNativeHandle(), uri, event);
    }

    /**
     * Invoke live data update event handler.
     *
     * @param uri            extension URI.
     * @param liveDataUpdate update message.
     * @return true if succeeded, false otherwise.
     */
    public boolean invokeLiveDataUpdate(String uri, String liveDataUpdate) {
        return nInvokeLiveDataUpdate(getNativeHandle(), uri, liveDataUpdate);
    }

    private native long nCreate(String uri_);
    private static native boolean nInvokeExtensionEventHandler(long handler_, String uri_, String event_);
    private static native boolean nInvokeLiveDataUpdate(long handler_, String uri_, String liveDataUpdate_);
    private static native void nRegistrationResult(long handler_, String uri_, String registrationResult_);
    private static native void nCommandResult(long handler_, String uri_, String commandResult_);
    private static native String nGetUri(long handler_);
}
