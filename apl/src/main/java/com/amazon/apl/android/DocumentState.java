/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

import com.amazon.apl.android.scaling.MetricsTransform;

/**
 * Maintains a reference to necessary objects needed to restore the document from a previous
 * state.
 */
public class DocumentState extends BoundObject {
    @NonNull
    private APLOptions mOptions;
    private final MetricsTransform mMetricsTransform;
    private final RootConfig mRootConfig;

    /**
     * Creates a document state to cache.
     * @param rootContext the inflated RootContext for the document.
     */
    public DocumentState(@NonNull RootContext rootContext) {
        bind(rootContext.getNativeHandle());
        mOptions = rootContext.getOptions();
        mMetricsTransform = rootContext.getMetricsTransform();
        mRootConfig = rootContext.getRootConfig();
    }

    /**
     * Set the {@link APLOptions} for this Document.
     * @param options
     */
    public void setOptions(@NonNull APLOptions options) {
        mOptions = options;
    }

    /**
     * @return the {@link APLOptions} associated with this Document.
     */
    @NonNull
    public APLOptions getOptions() {
        return mOptions;
    }

    /**
     * @return the {@link MetricsTransform} associated with this Document.
     */
    MetricsTransform getMetricsTransform() {
        return mMetricsTransform;
    }

    /**
     * @return the {@link RootConfig} associated with this Document.
     */
    RootConfig getRootConfig() {
        return mRootConfig;
    }
}
