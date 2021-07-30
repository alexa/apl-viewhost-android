/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import android.util.LruCache;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Bitmap drawable manager. LruCache stores { key: Bitmap }, return existing Bitmap
 * if key exists, otherwise return null.
 *
 * reference link: https://developer.android.com/topic/performance/graphics/manage-memory
 *
 */
public class LruBitmapCache implements IBitmapCache, ComponentCallbacks2 {

    private static final String TAG = "LruBitmapCache";
    // 1/32 of runtime available memory
    private static final int MEMORY_ALLOCATION = (int)(Runtime.getRuntime().maxMemory() / 1024 / 32);
    private static final Set<WeakReference<Bitmap>> mReusableBitmaps =
            Collections.synchronizedSet(new HashSet<>());

    private final LruCache<BitmapKey, Bitmap> mMemoryCache;

    /**
     * Create a new {@link LruBitmapCache}.
     *
     * Set default maxMemory
     *
     */
    public LruBitmapCache() {
        this(MEMORY_ALLOCATION);
    }

    /**
     * Create a new {@link LruBitmapCache} with specific maximum memory size.
     *
     * @param size Max memory that can be used by the cache
     */
    public LruBitmapCache(int size) {
        mMemoryCache = new LruCache<BitmapKey, Bitmap>(size) {

            @Override
            protected int sizeOf(BitmapKey key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }

            @Override
            protected void entryRemoved(
                    boolean evicted,
                    BitmapKey key,
                    Bitmap oldValue,
                    Bitmap newValue) {
                mReusableBitmaps.add(new WeakReference<>(oldValue));
            }
        };
    }

    /**
     * @inheritDoc
     */
    @Override
    public void putBitmap(BitmapKey key, Bitmap bitmap) {
        if (mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * @inheritDoc
     */
    @Nullable
    @Override
    public Bitmap getBitmap(BitmapKey key) {
        return mMemoryCache.get(key);
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getSize() {
        return mMemoryCache.size();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void clear() {
        mMemoryCache.evictAll();
    }

    @Override
    public void onTrimMemory(int level) {
        //System is low on memory so we should clear the cache as its not critical to having a functioning experience
        if(level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            clear();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }
}
