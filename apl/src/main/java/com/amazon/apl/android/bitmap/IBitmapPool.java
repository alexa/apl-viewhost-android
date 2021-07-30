/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;

/**
 * Bitmap pool interface.  Allows for recycling existing bitmap objects.
 */
public interface IBitmapPool {

    /**
     * Gets a "dirty" bitmap from the pool.  The bitmap is retrieved from the pool without clearing
     * and may contain graphics from previous use.  It is up to the caller to fully fill the bitmap
     * with desired content.
     *
     * @param width width
     * @param height height
     * @param config config
     * @return "dirty" Bitmap
     */
    Bitmap getDirty(int width, int height, Bitmap.Config config);

    /**
     * Get a "clean" bitmap from the pool.  The bitmap is cleared (zeroed) before returning.
     *
     * @param width width
     * @param height height
     * @param config config
     * @return "clean" Bitmap
     */
    Bitmap get(int width, int height, Bitmap.Config config);

    /**
     * Put a Bitmap back in the pool.
     *
     * @param bitmap bitmap to put back in the pool
     */
    void put(Bitmap bitmap);

}
