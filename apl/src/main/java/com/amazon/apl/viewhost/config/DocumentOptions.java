/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import static com.amazon.apl.android.ExtensionMediator.IExtensionGrantRequestCallback;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.google.auto.value.AutoValue;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Options that determine how a document will be rendered by the APL engine.
 */
@AutoValue
public abstract class DocumentOptions {
    @Nullable
    public abstract IExtensionGrantRequestCallback getExtensionGrantRequestCallback();

    /**
     * Specifies extensions supported by the runtime. If provided, it will be used instead of the
     * one in ViewhostConfig. In practice, this is required for legacy extension (V1) support.
     */
    @Nullable
    public abstract ExtensionRegistrar getExtensionRegistrar();

    @Nullable
    public abstract Map<String, Object> getExtensionFlags();

    @Nullable
    public abstract ITelemetryProvider getTelemetryProvider();

    @Nullable
    public abstract EmbeddedDocumentFactory getEmbeddedDocumentFactory();

    @Nullable
    public abstract IUserPerceivedFatalCallback getUserPerceivedFatalCallback();

    public static Builder builder() {
        return new AutoValue_DocumentOptions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder extensionGrantRequestCallback(IExtensionGrantRequestCallback callback);
        public abstract Builder extensionRegistrar(ExtensionRegistrar registrar);
        public abstract Builder extensionFlags(Map<String, Object> flags);

        public abstract Builder telemetryProvider(ITelemetryProvider telemetryProvider);

        public abstract Builder embeddedDocumentFactory(EmbeddedDocumentFactory embeddedDocumentFactory);

        public abstract Builder userPerceivedFatalCallback(IUserPerceivedFatalCallback userPerceivedFatalCallback);

        public abstract DocumentOptions build();
    }
}
