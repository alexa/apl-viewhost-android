/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.providers;

/**
 * Provider for {@link IDataRetriever}
 */
public interface IDataRetrieverProvider {
    /**
     * Gets an instance of {@link IDataRetriever}
     * @return instance of {@link IDataRetriever}
     */
    IDataRetriever get();
}
