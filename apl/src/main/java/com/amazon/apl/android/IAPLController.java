/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.enums.DisplayState;

import java.util.Map;

/**
 * Public API for interacting with an APL document.
 */
public interface IAPLController {

    /**
     * Callback for the Action associated with invoking an ExtensionEvent.
     */
    interface ExtensionEventHandlerCallback {
        void onExtensionEventInvoked(@NonNull Action action);
    }

    /**
     * Callback for whether a DataSource update was successful.
     */
    interface UpdateDataSourceCallback {
        void onDataSourceUpdate(boolean success);
    }

    /**
     * Callback for the Action associated with invoking ExecuteCommands.
     */
    interface ExecuteCommandsCallback {
        void onExecuteCommands(@NonNull Action action);
    }

    /**
     * Callback for an error during inflation of an APL document.
     */
    interface InflationErrorCallback {
        void onError(Exception e);
    }

    /**
     * Return if a setting is present in the main template. See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#document_settings_property
     * @param propertyName  the property name
     * @return              if the setting is present.
     */
    boolean hasSetting(String propertyName);

    /**
     * Return a setting from the main template. See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#document_settings_property
     * @param propertyName  the property name
     * @param defaultValue  the fallback if not present
     * @return              the value if present otherwise the fallback.
     */
    <K> K optSetting(String propertyName, K defaultValue);

    /**
     * Execute APL commands against the document.
     *
     * @param commands  the Commands to run.
     * @param callback  the callback to receive an Action reference if the command doesn't resolve instantly.
     */
    void executeCommands(@NonNull String commands, @Nullable ExecuteCommandsCallback callback);

    /**
     * Invoke an extension event handler.
     *
     * @param uri      The URI of the custom document handler
     * @param name     The name of the handler to invoke
     * @param data     The data to associate with the handler
     * @param fastMode If true, this handler will be invoked in fast mode
     * @param callback  the callback to receive an Action reference if the command doesn't resolve instantly.
     */
    void invokeExtensionEventHandler(@NonNull String uri, @NonNull String name,
                                     Map<String, Object> data, boolean fastMode, @Nullable ExtensionEventHandlerCallback callback);

    /**
     * @deprecated Use {@link #invokeExtensionEventHandler(String, String, Map, boolean, ExtensionEventHandlerCallback)} to ensure thread safety
     * and proper enqueueing of commands.
     *
     * Invoke an extension event handler.
     *
     * @param uri      The URI of the custom document handler
     * @param name     The name of the handler to invoke
     * @param data     The data to associate with the handler
     * @param fastMode If true, this handler will be invoked in fast mode
     * @return An Action.
     */
    @Nullable
    @Deprecated
    Action invokeExtensionEventHandler(@NonNull String uri, @NonNull String name, Map<String, Object> data, boolean fastMode) throws IllegalStateException;

    /**
     * Updates data source for the document.
     *
     * @param type DataSource type, should be one of the types registered with {@link RootConfig#registerDataSource(String)}
     * @param data an incremental data update
     * @param callback a callback to indicate success or failure of the update
     */
    void updateDataSource(@NonNull String type, @NonNull String data, @Nullable UpdateDataSourceCallback callback);

    /**
     * Updates the display state for this document. See [link to public doc]
     *
     * @param displayState the display state.
     */
    void updateDisplayState(DisplayState displayState);

    /**
     * @return Get the apl version of the document. See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-document.html#apl_document_version_property
     */
    int getDocVersion();

    /**
     * Cancels the main sequencer. See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-commands.html#command_sequencing
     */
    void cancelExecution();

    /**
     * Pauses a document, preventing any UI updates or events from being generated.
     */
    void pauseDocument();

    /**
     * Resumes a document, enabling UI updates and event processing again.
     */
    void resumeDocument();

    /**
     * Finishes the APL document. A typical place for doing this is when the activity is being destroyed
     * or before rendering a new APL document into the same {@link APLLayout}.
     *
     * Note: After finishing the document any other commands invoked on this {@link APLController} will be no-ops.
     */
    void finishDocument();

    /**
     * Execute task on Core handling thread.
     * @param task task to execute.
     */
    default void executeOnCoreThread(Runnable task) {
        task.run();
    }

    /**
     * @return true if document was finished, false otherwise.
     */
    default boolean isFinished() { return false; }

    // TODO Currently used by the Backstack but should be refactored out once BackExtension is moved into
    //  c++.
    DocumentState getDocumentState();
}
