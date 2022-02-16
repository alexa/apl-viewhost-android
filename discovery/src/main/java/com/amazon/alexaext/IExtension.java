/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import java.util.Set;

/**
 * Local extension interface.
 */
public interface IExtension {
    interface IExtensionEventCallback {
        boolean sendExtensionEvent(String uri, String event);
    }

    interface IExtensionCommandResultCallback {
        void sendCommandResult(String uri, String event);
    }

    /**
     * Get the URIs supported by the extension.
     *
     * @return The extension URIs.
     */
    Set<String> getURIs();

    /**
     * Create a registration for the extension. The registration is returned in a "RegistrationSuccess" or
     * "RegistrationFailure" message. The extension is defined by a unique token per registration, an environment of
     * static properties, and the extension schema.
     *
     * The schema defines the extension api, including commands, events and live data.  The "RegistrationRequest"
     * parameter contains a schema version, which matches the schema versions supported by the runtime, and extension
     * settings defined by the requesting document.
     *
     * std::exception or ExtensionException thrown from this method are converted to "RegistrationFailure"
     * messages and returned to the caller.
     *
     * This method is called by the extension framework when the extension is requested by a document.
     *
     * @param uri The extension URI.
     * @param registrationRequest A "RegistrationRequest" message, includes extension settings.
     * @return A extension "RegistrationSuccess" or "RegistrationFailure"  message.
     */
    String createRegistration(String uri, String registrationRequest);

    /**
     * Callback registration for extension "Event" messages. Guaranteed to be called before the document is mounted.
     * The callback forwards events to the document event handlers.
     *
     * @param callback The callback for events generating from the extension.
     */
    void registerEventCallback(IExtensionEventCallback callback);

    /**
     * Callback for extension "LiveDataUpdate" messages. Guaranteed to be called before the document is mounted.
     * The callback forwards live data changes to the document data binding and live data handlers.
     *
     * @param callback The callback for live data updates generating from the extension.
     */
    void registerLiveDataUpdateCallback(ILiveDataUpdateCallback callback);

    /**
     * Callback for extension command results. Guaranteed to be called before the document is mounted.
     * The callback forwards command results to the document.
     *
     * @param callback The callback for live data updates generating from the extension.
     */
    void registerCommandResultCallback(IExtensionCommandResultCallback callback);

    /**
     * Execute a Command that was initiated by the document.
     *
     * std::exception or ExtensionException thrown from this method are converted to "CommandFailure"
     * messages and returned to the caller.
     *
     * @param uri The extension URI.
     * @param command The requested Command message.
     * @return true if the command succeeded.
     */
    default boolean onCommand(int id, String uri, String command)  { return false; }

    /**
     * Process message requested by a component. DOes not require an answer.
     * @param uri The extension URI.
     * @param message The requested Command message.
     * @return tru if succeeds, false otherwise.
     */
    default boolean onMessage(String uri, String message) { return false; }

    /**
     * Notify extension about Resource being ready for use.
     * @param uri The extension URI.
     * @param resourceHolder corresponding resource holder.
     */
    default void onResourceReady(String uri, ResourceHolder resourceHolder) {}

    /**
     * Invoked after registration has been completed successfully. This is useful for
     * stateful extensions that require initializing session data upfront.
     *
     * @param uri The extension URI used during registration.
     * @param token The client token issued during registration.
     */
    default void onRegistered(String uri, String token) {}

    /**
     * Notification to extension on when it was un-registered.
     * @param uri URI
     * @param token Registration token.
     */
    default void onUnregistered(String uri, String token) {}
}
