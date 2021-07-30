/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Class for creating extension registration requests, event requests, and handling extension messages.
 */
public class ExtensionClient extends BoundObject {

    /**
     * Create an ExtensionClient for creating registration requests, event request, and processing
     * extension messages.
     * @param rootConfig    the rootconfig
     * @param uri           the extension uri
     * @return              an ExtensionClient
     */
    public static ExtensionClient create(@NonNull RootConfig rootConfig, @NonNull final String uri) {
        return new ExtensionClient(rootConfig, uri);
    }

    /**
     * Create a Registration request for the extension.
     * @param content   the content
     * @return          the registration request
     */
    public String createRegistrationRequest(final Content content) {
        return nCreateRegistrationRequestFromContent(getNativeHandle(), content.getNativeHandle());
    }

    /**
     * Create a Registration request for the extension.
     * @param uri       the uri
     * @param settings  a Map of settings
     * @return          the registration request
     */
    public static String createRegistrationRequest(final String uri, final Map<String,Object> settings) {
        return nCreateRegistrationRequestFromMap(uri, settings);
    }

    /**
     * @return if the registration message has been received.
     */
    public boolean registrationMessageProcessed() {
        return nRegistrationMessageProcessed(getNativeHandle());
    }

    /**
     * @return if the extension successfully registered with core.
     */
    public boolean registered() {
        return nRegistered(getNativeHandle());
    }

    /**
     * Process the message received from an Extension.
     * @param rootContext   the RootContext (may be null
     * @param message       the message from the Extension.
     * @return              true if the message is processed.
     */
    public boolean processMessage(@Nullable final RootContext rootContext, @NonNull final String message) {
        return nProcessMessage(getNativeHandle(), rootContext == null ? 0 : rootContext.getNativeHandle(), message);
    }

    /**
     * Create a message for an Extension from an event.
     * @param event the extension event.
     * @return      the message to send to the Extension.
     */
    public String processCommand(@NonNull final Event event) {
        return nProcessCommand(getNativeHandle(), event.getNativeHandle());
    }

    private ExtensionClient(@NonNull RootConfig rootConfig, @NonNull final String uri) {
        final long handle = nCreate(rootConfig.getNativeHandle(), uri);
        bind(handle);
    }

    private static native long nCreate(long rootConfig_, String uri_);
    private static native String nCreateRegistrationRequestFromContent(long handle, long content_);
    private static native String nCreateRegistrationRequestFromMap(String uri, Map<String, Object> settings);
    private static native boolean nRegistrationMessageProcessed(long handle);
    private static native boolean nRegistered(long handle);
    private static native boolean nProcessMessage(long handle, long rootContext_, String message);
    private static native String nProcessCommand(long handle, long event_);
}
