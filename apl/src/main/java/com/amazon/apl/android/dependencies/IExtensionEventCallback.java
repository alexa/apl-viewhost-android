/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies;

import com.amazon.apl.android.Event;

import java.util.Map;

public interface IExtensionEventCallback {

    interface IExtensionEventCallbackResult {
        /**
         * Called when the extension requires resolve.
         *
         * @param succeeded `true` if succeeded. If false is returned, then the command
         *                  fails, which dispatches the TODO
         */
        void onResult(boolean succeeded);
    }

    /**
     * Callback for Extension Events.
     *
     * @param name           The name of the extension assigned by the APL document.
     * @param uri            The URI of the extension.
     * @param event          Event reference
     * @param source         Map of the source object that raised the event.
     * @param custom         Map of the user-specified properties listed at registration time.
     * @param resultCallback NULL if the extension command does not require resolution.  Call this
     *                       with the result of the extension event execution.
     */
    default void onExtensionEvent(String name, String uri,
                          Event event,
                          Map<String, Object> source,
                          Map<String, Object> custom,
                          IExtensionEventCallbackResult resultCallback) {
        onExtensionEvent(name, uri, source, custom, resultCallback);
    }

    /**
     * Callback for Extension Events.
     *
     * @deprecated use {@link #onExtensionEvent(String, String, Event, Map, Map, IExtensionEventCallbackResult)}.
     *
     * @param name           The name of the extension assigned by the APL document.
     * @param uri            The URI of the extension.
     * @param source         Map of the source object that raised the event.
     * @param custom         Map of the user-specified properties listed at registration time.
     * @param resultCallback NULL if the extension command does not require resolution.  Call this
     *                       with the result of the extension event execution.
     */
    @Deprecated
    void onExtensionEvent(String name, String uri,
                          Map<String, Object> source,
                          Map<String, Object> custom,
                          IExtensionEventCallbackResult resultCallback);
}
