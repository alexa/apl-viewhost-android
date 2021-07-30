/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import android.content.Context;
import androidx.annotation.Nullable;

import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.dependencies.impl.NoOpImageLoader;
import com.amazon.apl.android.providers.IImageLoaderProvider;

public class NoOpImageLoaderProvider implements IImageLoaderProvider {
    private static NoOpImageLoaderProvider INSTANCE;

    private NoOpImageLoaderProvider() {}

    public static NoOpImageLoaderProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpImageLoaderProvider();
        }
        return INSTANCE;
    }

    @Nullable
    @Override
    public IImageLoader get(Context context) {
        return new NoOpImageLoader();
    }

    @Override
    public void onDocumentFinish() {

    }
}
