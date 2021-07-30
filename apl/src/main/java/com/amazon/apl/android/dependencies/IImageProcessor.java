/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.primitive.Filters;

import java.util.List;

/**
 * ImageProcessor dependency.
 */
public interface IImageProcessor {
    /**
     * Convenience ImageProcessor to apply before {@link Filters} are processed.
     *
     * A better approach is to use {@link IExtensionImageFilterCallback} and provide
     * filter processing in an extension.
     *
     * @param sources   a list of sources defined in the document {@link Image#getSources()}
     * @param bitmaps   a list of bitmaps before filters have been applied.
     * @return          the processed bitmaps (must have the same order and size as {@param bitmaps}).
     */
    @NonNull
    List<Bitmap> preProcessImage(@NonNull List<String> sources, @NonNull List<Bitmap> bitmaps);
}