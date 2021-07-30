/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.providers.impl;


import androidx.annotation.NonNull;

import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetrieverProvider;

/**
 * Default provider
 */
public class HttpRetrieverProvider implements IDataRetrieverProvider {
    /**
     * Returns an instance of {@link IDataRetriever}
     * @return
     */
    @NonNull
    public IDataRetriever get() {
        return new HttpRetriever();
    }
}
