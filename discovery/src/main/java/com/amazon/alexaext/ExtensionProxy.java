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
    protected abstract boolean initialize(String uri);

    /**
     * Process request to invoke a command. To be overriden by specific implementation.
     * Result needs to be returned by calling @see ExtensionProxy::commandResult with appropriate parameters.
     *
     * @param activity ActivityDescriptor.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean invokeCommand(ActivityDescriptor activity, String command);

    /**
     * Process request to pass a message. To be overriden by specific implementation.
     * No response is expected.
     *
     * @param activity ActivityDescriptor.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean sendMessage(ActivityDescriptor activity, String command);

    /**
     * Process registration request. To be overriden by specific implementation.
     * Result needs to be returned by calling @see ExtensionProxy::registrationResult with appropriate parameters.
     *
     * @param activity ActivityDescriptor.
     * @return true if successful, false otherwise.
     */
    protected abstract boolean requestRegistration(ActivityDescriptor activity, String request);

    /**
     * Called when extension was properly registered.
     *
     * @paramactivity ActivityDescriptor.
     */
    protected abstract void onRegistered(ActivityDescriptor activity);

    /**
     * Invoked when an extension is unregistered. Session represented by the provided token is no longer valid.
     *
     * @param activity ActivityDescriptor.
     */
    protected void onUnregistered(ActivityDescriptor activity) {}


    /**
     * Invoked when a system rendering resource, such as display surface, is ready for use. This method
     * will be called after the viewhost receives a "Component" message with a resource state of
     * "Ready".  Not all execution environments support shared rendering resources.
     *
     * @param activity ActivityDescriptor.
     * @param resourceHolder Access to the rendering resource.
     */
    protected void onResourceReady(ActivityDescriptor activity, ResourceHolder resourceHolder) {
        // default to no extension support
    }

    /**
     * @see {@link IExtension::onSessionStarted}
     */
    protected void onSessionStarted(SessionDescriptor session) {}

    /**
     * @see {@link IExtension::onSessionEnded}
     */
    protected void onSessionEnded(SessionDescriptor session) {}

    /**
     * @see {@link IExtension::onForeground}
     */
    protected void onForeground(ActivityDescriptor activity) {}

    /**
     * @see {@link IExtension::onBackground}
     */
    protected void onBackground(ActivityDescriptor activity) {}

    /**
     * @see {@link IExtension::onHidden}
     */
    protected void onHidden(ActivityDescriptor activity) {}

    @NonNull
    @SuppressWarnings("unused")
    @VisibleForTesting
    public boolean initializeNative(String uri) {
        return initialize(uri);
    }

    @NonNull
    @SuppressWarnings("unused")
    @VisibleForTesting
    public boolean requestRegistrationNative(ActivityDescriptor activity, String request) {
        return requestRegistration(activity, request);
    }

    @NonNull
    @SuppressWarnings("unused")
    @VisibleForTesting
    public boolean invokeCommandNative(ActivityDescriptor activity, String command) {
        return invokeCommand(activity, command);
    }

    @NonNull
    @SuppressWarnings("unused")
    private boolean sendMessageNative(ActivityDescriptor activity, String command) {
        return sendMessage(activity, command);
    }

    @NonNull
    @SuppressWarnings("unused")
    private void onRegisteredNative(ActivityDescriptor activity) {
        onRegistered(activity);
    }

    @NonNull
    @SuppressWarnings("unused")
    private void onUnregisteredNative(ActivityDescriptor activity) {
        onUnregistered(activity);
    }

    @NonNull
    @SuppressWarnings("unused")
    void onResourceReadyNative(ActivityDescriptor activity, ResourceHolder resourceHolder) {
        onResourceReady(activity, resourceHolder);
    }

    @NonNull
    @SuppressWarnings("unused")
    void onSessionStartedNative(SessionDescriptor session) {
        onSessionStarted(session);
    }

    @NonNull
    @SuppressWarnings("unused")
    void onSessionEndedNative(SessionDescriptor session) {
        onSessionEnded(session);
    }

    @NonNull
    @SuppressWarnings("unused")
    void onForegroundNative(ActivityDescriptor activity) {
        onForeground(activity);
    }

    @NonNull
    @SuppressWarnings("unused")
    void onBackgroundNative(ActivityDescriptor activity) {
        onBackground(activity);
    }

    @NonNull
    @SuppressWarnings("unused")
    void onHiddenNative(ActivityDescriptor activity) {
        onHidden(activity);
    }

    /**
     * @return extension URI.
     */
    public String getUri() {
        return nGetUri(getNativeHandle());
    }

    public void registrationResult(ActivityDescriptor activity, String result) {
        if (activity == null) return;
        nRegistrationResult(getNativeHandle(), activity.getURI(), activity.getSession().getId(), activity.getActivityId(), result);
    }

    /**
     * Pass registration result to the core.
     *
     * @param activity ActivityDescriptor.
     * @param result result message.
     */
    public void commandResult(ActivityDescriptor activity, String result) {
        if (activity == null) return;
        nCommandResult(getNativeHandle(), activity.getURI(), activity.getSession().getId(), activity.getActivityId(), result);
    }

    /**
     * Invoke extension event handler.
     *
     * @param activity ActivityDescriptor.
     * @param event event message.
     * @return true if succeeded, false otherwise.
     */
    public boolean invokeExtensionEventHandler(ActivityDescriptor activity, String event) {
        if (activity == null) return false;
        return nInvokeExtensionEventHandler(getNativeHandle(), activity.getURI(), activity.getSession().getId(), activity.getActivityId(), event);
    }

    /**
     * Invoke live data update event handler.
     *
     * @param activity ActivityDescriptor.
     * @param liveDataUpdate update message.
     * @return true if succeeded, false otherwise.
     */
    public boolean invokeLiveDataUpdate(ActivityDescriptor activity, String liveDataUpdate) {
        if (activity == null) return false;
        return nInvokeLiveDataUpdate(getNativeHandle(), activity.getURI(), activity.getSession().getId(), activity.getActivityId(), liveDataUpdate);
    }

    private native long nCreate(String uri_);
    private static native boolean nInvokeExtensionEventHandler(long handler_, String uri_, String sessionId, String activityId_, String event_);
    private static native boolean nInvokeLiveDataUpdate(long handler_, String uri_, String sessionId, String activityId_, String liveDataUpdate_);
    private static native void nRegistrationResult(long handler_, String uri_, String sessionId, String activityId_, String registrationResult_);
    private static native void nCommandResult(long handler_, String uri_, String sessionId, String activityId_, String commandResult_);
    private static native String nGetUri(long handler_);
}
