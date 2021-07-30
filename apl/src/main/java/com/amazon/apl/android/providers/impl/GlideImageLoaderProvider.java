/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import android.content.Context;
import androidx.annotation.Nullable;

import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.dependencies.impl.GlideImageLoader;
import com.amazon.apl.android.providers.IImageLoaderProvider;

/**
 * GlideImageLoader provider.
 */
public class GlideImageLoaderProvider implements IImageLoaderProvider {

    @Nullable
    private IImageLoader mImageLoader;

    public GlideImageLoaderProvider() {
    }

    /**
     * TODO: Consider to change this signature to get() and pass the Android context
     * through constructor, as ImageLoader client is a singleton and any
     * different context is ignored.
     * * Similar to TTS approach.
     * * This is a customer breaking change.
     * 
     * Gets a ImageLoader instance.
     * @return Returns a stored instance of the ImageLoader
     */
    @Override
    public synchronized IImageLoader get(Context context) {
        if(mImageLoader == null) {
            mImageLoader = new GlideImageLoader(context);
        }
        return mImageLoader;
    }



    /**
     * The document is no longer valid for display.
     */
    @Override
    public void onDocumentFinish() {
        if (mImageLoader != null) {
            mImageLoader.clearResources();
        }
    }
}
