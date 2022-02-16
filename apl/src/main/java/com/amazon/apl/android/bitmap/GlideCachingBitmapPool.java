/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

/**
 * Caching bitmap pool.
 *
 * This is a wrapper around Glide's BitmapPool.  Glide is a fast and efficient image loading
 * library for Android focused on smooth scrolling.
 *
 * See more: https://bumptech.github.io/glide/
 */
public class GlideCachingBitmapPool implements IBitmapPool {
    private final BitmapPool pool;
    private static final String TAG = "GlideCachingBitmapPool";

    /**
     * Create a caching bitmap pool from a given Android context.
     *
     * @param context Android context
     * @return new GlideCachingBitmapPool instance
     */
    public static GlideCachingBitmapPool fromContext(Context context) {
        return new GlideCachingBitmapPool(Glide.get(context).getBitmapPool());
    }

    /**
     * Create a new instance from a given Glide bitmap pool.
     *
     * @param bitmapPool bitmap pool
     */
    public GlideCachingBitmapPool(BitmapPool bitmapPool) {
        this.pool = bitmapPool;
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
