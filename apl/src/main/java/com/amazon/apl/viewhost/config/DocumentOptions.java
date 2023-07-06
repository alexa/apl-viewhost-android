/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import static com.amazon.apl.android.ExtensionMediator.IExtensionGrantRequestCallback;

import com.amazon.apl.android.dependencies.IOpenUrlCallback;
import com.google.auto.value.AutoValue;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Options that determine how a document will be rendered by the APL engine.
 */
@AutoValue
public abstract class DocumentOptions {
    @Nullable
    public abstract IExtensionGrantRequestCallback getExtensionGrantRequestCallback();

    @Nullable
    public abstract Map<String, Object> getExtensionFlags();

    public static Builder builder() {
        return new AutoValue_DocumentOptions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder extensionGrantRequestCallback(IExtensionGrantRequestCallback callback);
        public abstract Builder extensionFlags(Map<String, Object> flags);
        public abstract DocumentOptions build();
    }
}
