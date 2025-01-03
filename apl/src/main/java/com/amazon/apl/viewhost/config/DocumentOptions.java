/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import static com.amazon.apl.android.ExtensionMediator.IExtensionGrantRequestCallback;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.google.auto.value.AutoValue;

import java.util.HashMap;
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
    public abstract MetricsOptions getMetricsOptions();

    @Nullable
    public abstract EmbeddedDocumentFactory getEmbeddedDocumentFactory();

    @Nullable
    public abstract IUserPerceivedFatalCallback getUserPerceivedFatalCallback();

    @Nullable
    public abstract Map<String, Object> getProperties();
    public static Builder builder() {
        return new AutoValue_DocumentOptions.Builder().properties(new HashMap<>());
    }


    /**
     * Merges the current DocumentOptions instance with another DocumentOptions instance.
     * The fields of the other instance take precedence over the fields of the current instance.
     *
     * @param other the DocumentOptions instance to merge with
     * @return a new DocumentOptions instance with merged fields
     */
    public DocumentOptions merge(DocumentOptions other) {
        if (other == null) {
            return this;
        }

        DocumentOptions.Builder builder = DocumentOptions.builder();

        builder.extensionGrantRequestCallback(
                chooseValue(other.getExtensionGrantRequestCallback(), getExtensionGrantRequestCallback())
        );

        builder.extensionRegistrar(
                chooseValue(other.getExtensionRegistrar(), getExtensionRegistrar())
        );

        builder.extensionFlags(
                mergeMaps(getExtensionFlags(), other.getExtensionFlags())
        );

        builder.telemetryProvider(
                chooseValue(other.getTelemetryProvider(), getTelemetryProvider())
        );

        builder.metricsOptions(
                chooseValue(other.getMetricsOptions(), getMetricsOptions())
        );

        builder.embeddedDocumentFactory(
                chooseValue(other.getEmbeddedDocumentFactory(), getEmbeddedDocumentFactory())
        );

        builder.userPerceivedFatalCallback(
                chooseValue(other.getUserPerceivedFatalCallback(), getUserPerceivedFatalCallback())
        );

        builder.properties(
                mergeMaps(getProperties(), other.getProperties())
        );

        return builder.build();
    }

    private <T> T chooseValue(T provided, T defaultValue) {
        return provided != null ? provided : defaultValue;
    }

    private Map<String, Object> mergeMaps(Map<String, Object> defaultMap, Map<String, Object> providedMap) {
        Map<String, Object> mergedMap = new HashMap<>();
        if (defaultMap != null) {
            mergedMap.putAll(defaultMap);
        }
        if (providedMap != null) {
            mergedMap.putAll(providedMap);
        }
        return mergedMap;
    }
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder extensionGrantRequestCallback(IExtensionGrantRequestCallback callback);
        public abstract Builder extensionRegistrar(ExtensionRegistrar registrar);
        public abstract Builder extensionFlags(Map<String, Object> flags);

        public abstract Builder telemetryProvider(ITelemetryProvider telemetryProvider);
        public abstract Builder metricsOptions(MetricsOptions metricsOptions);

        public abstract Builder embeddedDocumentFactory(EmbeddedDocumentFactory embeddedDocumentFactory);

        public abstract Builder userPerceivedFatalCallback(IUserPerceivedFatalCallback userPerceivedFatalCallback);

        public abstract Builder properties(Map<String, Object> properties);

        public abstract DocumentOptions build();
    }
}