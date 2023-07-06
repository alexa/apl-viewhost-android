/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost;

import android.view.View;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.internal.ViewhostImpl;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

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

    // TODO: Add APIs for view management (bind, unbind, isBound)
    // TODO: Add APIs for platform events (updateDisplayState, cancelExecution, configurationChange)
    // TODO: Add listeners for
}