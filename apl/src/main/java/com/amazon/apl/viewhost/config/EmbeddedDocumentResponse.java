/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import com.amazon.apl.viewhost.PreparedDocument;
import com.google.auto.value.AutoValue;

/**
 * Builds an embedded document response for use in EmbeddedDocumentFactory
 */
@AutoValue
public abstract class EmbeddedDocumentResponse {
    /**
     * The embedded document that was requested
     */
    public abstract PreparedDocument getPreparedDocument();

    /**
     * Whether the visual context of this embedded document is considered "attached" to its parent.
     * If so, its visual context will be stitched into its parent's visual context.
     */
    public abstract boolean isVisualContextAttached();

    public static Builder builder() {
        return new AutoValue_EmbeddedDocumentResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder preparedDocument(PreparedDocument document);
        public abstract Builder visualContextAttached(boolean isAttached);
        public abstract EmbeddedDocumentResponse build();
    }
}
