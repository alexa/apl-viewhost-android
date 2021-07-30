/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;

/**
 * Caching bitmap pool.
 *
 * This implementation uses LruBitmapPool from Glide, a third party library.
 *
 * See more: https://bumptech.github.io/glide/
 */
public class GlideCachingBitmapPool implements IBitmapPool {
    BitmapPool pool;

    /**
     * Create a bitmap pool with a given maximum pool size.
     *
     * @param maxPoolSize maximum pool size
     */
    public GlideCachingBitmapPool(long maxPoolSize) {
        pool = new LruBitmapPool(maxPoolSize);
    }

    @Override
    public Bitmap getDirty(int width, int height, Bitmap.Config config) {
        return pool.getDirty(width, height, config);
    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        return pool.get(width, height, config);
    }

    @Override
    public void put(Bitmap bitmap) {
        pool.put(bitmap);
    }
}
