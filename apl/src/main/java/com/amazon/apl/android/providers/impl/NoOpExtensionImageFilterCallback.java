/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import android.graphics.Bitmap;

import com.amazon.apl.android.dependencies.ExtensionFilterParameters;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;


/**
 * Default ExtensionImageFilterCallback that returns the
 */
public class NoOpExtensionImageFilterCallback implements IExtensionImageFilterCallback {

    @Override
    public Bitmap processImage(Bitmap source, Bitmap destination, ExtensionFilterParameters params) {
        return source;
    }
}
