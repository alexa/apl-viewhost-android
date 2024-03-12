/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.Action;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.internal.DocumentStateChangeListener;
import com.amazon.apl.viewhost.internal.SavedDocument;
import com.amazon.apl.viewhost.internal.ViewhostImpl;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

import java.util.Map;

public abstract class Viewhost {

    /**
     * Create an instance of Viewhost with a given configuration
     */
    public static Viewhost create(ViewhostConfig config) {
        return new ViewhostImpl(config);
    }

    /**
     * Prepare but do not render an APL document. The request is typically processed asynchronously.
     * The returned document may not be fully prepared once this call returns. The viewhost will
     * publish lifecycle events for this document to notify clients of its progress.
     *
     * @param request The request object describing the document to prepare.
     *
     * @return The prepared document, or @c nullptr if the prepare operation could not be attempted.
     */
    public abstract PreparedDocument prepare(PrepareDocumentRequest request);

    /**
     * Renders the document specified by the request. The request is typically processed
     * asynchronously. The returned document may not be rendered once this call returns. The
     * viewhost will publish lifecycle events for this document to notify clients of its progress.
     *
     * @param request The request object describing the document to render.
     *
     * @return the handle to the document, @c nullptr if rendering could not be attempted.
     */
    public abstract DocumentHandle render(RenderDocumentRequest request);

    /**
     * Renders a document that was previously prepared. Callers are not required to wait until the
     * prepared document is ready to call this method. If the document is still being prepared,
     * rendering still start once preparation is resolved. The request is typically processed
     * asynchronously. Asynchronous implementations additionally will not block until the document
     * is prepared before returning.
     *
     * The returned document may not be rendered once this call returns. The viewhost will publish
     * lifecycle events for this document to notify clients of its progress.
     *
     * @param preparedDocument The prepared document
     * @return the handle to the document, @c nullptr if rendering could not be attempted
     */
    public abstract DocumentHandle render(PreparedDocument preparedDocument);

    /**
     * Registers document state change listener.
     * @param listener
     */
    public abstract void registerStateChangeListener(DocumentStateChangeListener listener);

    /**
     * Binds the Viewhost instance to the native view or throws IllegalStateException if a view is already bound.
     * @param aplLayout native view
     */
    public abstract void bind(APLLayout aplLayout);

    /**
     * Unbinds the Viewhost instance.
     */
    public abstract void unBind();

    /**
     * Returns true if a view is bound to the Viewhost instance, false otherwise.
     * @return true/false
     */
    public abstract boolean isBound();
    /**
     * Updates the display state for this document. See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-data-binding-evaluation.html#displaystate
     * We will update the frame loop frequency according to the displayState.
     *
     * @param displayState the display state.
     */
    public abstract void updateDisplayState(DisplayState displayState);

    /**
     * Cancels the main sequencer. See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-commands.html#command_sequencing
     */
    public abstract void cancelExecution();

    /**
     * Restores the specified document (e.g. from the backstack). If a document is currently being rendered, it is
     * replaced.
     *
     * @param document  The document to restore.
     * @return boolean  Returns true when the request is valid and has been accepted to be restored. Returns false for an invalid request.
     */
    public abstract boolean restoreDocument(SavedDocument document);

    /**
     * Invoke an extension event handler.
     *
     * @param uri      The URI of the custom document handler
     * @param name     The name of the handler to invoke
     * @param data     The data to associate with the handler
     * @param fastMode If true, this handler will be invoked in fast mode
     * @param callback  the callback to receive an Action reference if the command doesn't resolve instantly.
     */
    public abstract void invokeExtensionEventHandler(@NonNull String uri, @NonNull String name,
                                                     Map<String, Object> data, boolean fastMode, @Nullable ExtensionEventHandlerCallback callback);

    /**
     * Callback for the Action associated with invoking an ExtensionEvent.
     */
    public interface ExtensionEventHandlerCallback {
        /**
         * Called when the extension event was handled successfully
         */
        void onComplete();

        /**
         * Called when the extension event handling was terminated before completion
         */
        void onTerminated();
    }

    // TODO: Add API for platform events (configurationChange)
    // TODO: Add listeners for
}