/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.widget.ImageView;

import com.amazon.apl.android.dependencies.IImageLoader;

public class NoOpImageLoader implements IImageLoader {
    @Override
    public void loadImage(String path, ImageView imageView, LoadImageCallback callback, boolean needsScaling) {

    }

    @Override
    public void loadImage(String path, ImageView imageView, LoadImageCallback callback, boolean needsScaling, ImageLoaderRequestOptions options) {

    }

    @Override
    public void clear(ImageView imageView) {

    }

    @Override
    public void cancel() {

    }
}
