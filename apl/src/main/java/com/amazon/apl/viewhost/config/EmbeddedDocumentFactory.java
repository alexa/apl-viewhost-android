/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;

/**
 * Interface for APL to request embedded documents from the runtime and for the runtime to respond.
 */
public interface EmbeddedDocumentFactory {
    /**
     * Callback interface used by the runtime to respond to incoming document requests.
     */
    interface EmbeddedDocumentRequest {
        /**
         * @return The "source" property of a Host component
         */
        String getSource();

        /**
         * @return The handle of the requesting document
         */
        DocumentHandle getRequestingDocument();

        /**
         * Called to successfully fulfill the requested document.
         *
         * @param preparedDocument The requested document
         */
        @Deprecated
        void resolve(PreparedDocument preparedDocument);

        /**
         * Called to successfully fulfill the requested document.
         *
         * @param response The embedded response containing the requested document
         */
        void resolve(EmbeddedDocumentResponse response);

        /**
         * Called to mark the document request as failed
         *
         * @param reason The reason for failure
         */
        void fail(final String reason);
    }

    /**
     * Called by APL to request an embedded document
     */
    void onDocumentRequested(EmbeddedDocumentRequest request);
}