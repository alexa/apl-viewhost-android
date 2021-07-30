/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.listeners;

import android.view.View;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.utils.LazyImageLoader;
import com.amazon.apl.android.views.APLImageView;

/**
 * An implementation of View.OnAttachStateChangeListener that handles
 * loading and unloading of image resources
 */
public class OnImageAttachStateChangeListener implements View.OnAttachStateChangeListener {
    private static final String TAG = "OnImageAttachSCL";

    private ImageViewAdapter mImageViewAdapter;
    private Image mImage;
    private APLImageView mImageView;

    public OnImageAttachStateChangeListener(ImageViewAdapter imageViewAdapter, Image image, APLImageView imageView) {
        this.mImageViewAdapter = imageViewAdapter;
        this.mImage = image;
        this.mImageView = imageView;
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView);
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        LazyImageLoader.clearImageResources(mImageViewAdapter, mImage, mImageView);
    }
}
