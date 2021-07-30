/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers;

import android.content.Context;

import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.dependencies.IImageLoader;

/**
 * Provides an instance of ImageLoader.
 */
public interface IImageLoaderProvider extends IDocumentLifecycleListener {

    /**
     *
     * @param context The Android context.
     * @return An instance of a image loader.
     */
    IImageLoader get(Context context);

}
