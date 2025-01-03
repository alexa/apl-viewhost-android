/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.internal.DocumentStateChangeListener;
import com.amazon.apl.viewhost.internal.SavedDocument;
import com.amazon.apl.viewhost.internal.ViewhostImpl;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateViewStateRequest;

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
     *
     * @deprecated Use {@link Viewhost#updateViewState()} instead.
     */
    @Deprecated
    public abstract void updateDisplayState(DisplayState displayState);

    /**
     * Inform the view host about changes to the view state.
     *
     * @param request The request to update the view state
     */
    public abstract void updateViewState(UpdateViewStateRequest request);

    /**
     * Returns the view host's current display state.
     *
     *  - HIDDEN:     The view is not visible on the screen.
     *  - BACKGROUND: The view may be visible on the screen or it may be largely obscured by other
     *                content on the screen. The view is not the primary focus of the system.
     *  - FOREGROUND: The view is visible on the screen and at the front.
     */
    public abstract com.amazon.apl.viewhost.primitives.DisplayState getDisplayState();

    /**
     * Returns the view host's current processing rate, measured in cycles per second (Hz).
     *
     * - A negative value indicates the default frame rate for the device (unthrottled).
     * - A value of 0 means that the frame loop is stopped. Any incoming execute commands, dynamic
     *   data updates, extension messages will queued. This has the additional behavior of stopping
     *   elapsed time until a non-zero frame rate is specified. This usage is not normally
     *   recommended as it may produce an additional jank when processing is resumed.
     * - A positive value indicates an intention to operate at throttled (reduced) processing rate.
     *   This only has an impact if the value specified is less than the actual frame rate of the
     *   device. It cannot be used to increase the rate beyond the device's normal rate.
     */
    public abstract double getProcessingRate();

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
