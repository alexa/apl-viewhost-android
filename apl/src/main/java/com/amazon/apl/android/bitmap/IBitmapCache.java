/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;

public interface IBitmapCache {

    /**
     * Add {@link Bitmap} into cache with given {@link BitmapKey}.
     *
     * The BitmapKey is a unique identifier for a specific Bitmap and must
     * override the equals and hashcode methods
     *
     * @param key A unique identifier for this bitmap
     * @param bitmap The bitmap
     */
    void putBitmap(BitmapKey key, Bitmap bitmap);

    /**
     * Get the {@link Bitmap} with specific key
     *
     * @param key the key to return Bitmap of
     * @return a Bitmap
     */
    @Nullable
    Bitmap getBitmap(BitmapKey key);

    /**
     * Get cache size. This could be the number of entries in the cache but should
     * preferably be the total size in bytes for better memory tracking
     *
     * @return cache size
     */
    int getSize();

    /**
     * Clear the cache
     * Note: This is called in onDocumentFinish
     */
    void clear();

}
