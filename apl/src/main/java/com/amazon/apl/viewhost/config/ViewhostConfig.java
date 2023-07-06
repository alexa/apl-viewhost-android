/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.viewhost.message.MessageHandler;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.google.auto.value.AutoValue;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Configuration options for the viewhost
 */
@AutoValue
public abstract class ViewhostConfig {
    /**
     * Default document options for prepare/render document requests that do not supply their own.
     */
    @Nullable
    public abstract DocumentOptions getDefaultDocumentOptions();

    /**
     * Communication between the APL view host and the runtime relies on asynchronous message
     * passing. This defines a single handler to receive all view host messages.
     *
     * Alternatively, specify multiple message handlers with @see getMessageHandlers()
     */
    @Nullable
    public abstract MessageHandler getMessageHandler();

    /**
     * Define an ordered chain of message handlers. The first message handler in the chain that
     * returns true in its handler for a given message is considered to have consumed that message.
     * If the message handler returns false, the next handler in the chain is considered.
     *
     * If specified, the singular @see getMessageHandler() method is ignored.
     */
    @Nullable
    public abstract List<MessageHandler> getMessageHandlers();

    /**
     * Defines extensions supported by the runtime.
     */
    @Nullable
    public abstract ExtensionRegistrar getExtensionRegistrar();

    /**
     * Defines how to resolve requests for embedded documents
     */
    @Nullable
    public abstract EmbeddedDocumentFactory getEmbeddedDocumentFactory();

    /**
     * Defines how to resolve packages
     */
    @Nullable
    public abstract IPackageLoader getIPackageLoader();

    /**
     * Defines how to resolve content
     */
    @Nullable
    public abstract IContentRetriever getIContentRetriever();

    public static Builder builder() {
        return new AutoValue_ViewhostConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder defaultDocumentOptions(DocumentOptions options);
        public abstract Builder messageHandler(MessageHandler handler);
        public abstract Builder messageHandlers(List<MessageHandler> handler);
        public abstract Builder extensionRegistrar(ExtensionRegistrar registrar);
        public abstract Builder embeddedDocumentFactory(EmbeddedDocumentFactory factory);
        public abstract Builder IPackageLoader(IPackageLoader packageLoader);
        public abstract Builder IContentRetriever(IContentRetriever contentRetriever);
        public abstract ViewhostConfig build();
    }
}
