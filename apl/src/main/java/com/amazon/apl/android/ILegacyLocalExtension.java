/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.alexaext.ILiveDataUpdateCallback;
import com.amazon.apl.android.providers.IExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Combined "legacy" local extension interface.
 * Allows for easy support for LiveData and settings while using Legacy built-in extensions with the
 * new framework.
 */
@Deprecated
public interface ILegacyLocalExtension extends IExtension, com.amazon.apl.android.dependencies.IExtensionEventCallback {
    /**
     * Extension -> Document event propagation callback
     */
    interface ISendExtensionEventCallback {
        boolean sendExtensionEvent(String name, Map<String, Object> parameters);
    }

    /**
     * Supposed to be overriden if any LiveData is provided by an implementation.
     * @return List of LiveData adapters.
     */
    default List<LiveDataAdapter> getLiveData() {
        return new ArrayList<>();
    }

    /**
     * Initialize internal state/LiveData.
     * @param eventCallback event callback to call on extension events.
     * @param liveDataCallback Callback to pass to LiveData adapters.
     * @return true if successful, false otherwise.
     */
    default boolean initialize(ISendExtensionEventCallback eventCallback, ILiveDataUpdateCallback liveDataCallback) {
        return true;
    }

    /**
     * Notification to extension on when it was registered.
     * @param uri URI
     * @param token Registration token.
     */
    default void onRegistered(String uri, String token) {}

    /**
     * Invoked when an extension is unregistered. Session represented by the provided token is no longer valid.
     * @param uri URI
     * @param token Registration token.
     */
    default void onUnregistered(String uri, String token) {}

    /**
     * Apply document->requested settings. Supposed to be overriden by actual implementation.
     * @param settings settings map.
     */
    default void applySettings(Map<String, Object> settings) {}
}
