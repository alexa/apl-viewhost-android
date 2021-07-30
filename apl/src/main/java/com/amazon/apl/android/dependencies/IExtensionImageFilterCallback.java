/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IExtensionImageFilterCallback {
    /**
     * Callback for processing bitmaps.
     * It is important that the caller will not mutate the source or destination bitmaps as they may
     * be reused by other filters and can lead to undefined behavior.
     *
     * @param sourceBitmap      The source bitmap (immutable) or null.
     * @param destinationBitmap The destination bitmap (immutable) or null.
     * @param params            The ExtensionFilterParameters from the document.
     * @return                  A processed bitmap
     */
    @NonNull
    Bitmap processImage(@Nullable Bitmap sourceBitmap, @Nullable Bitmap destinationBitmap, ExtensionFilterParameters params);
}
