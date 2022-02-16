/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

/**
 * Simple LruCache that evicts all entries when we're about to be killed and trims to half the
 * size when we're backgrounded.
 * @param <K> the keys
 * @param <V> the values
 */
public abstract class LruCacheWrapper<K,V> implements ICache<K,V> {
    private final LruCache<K, V> mLruCacheInternal;

    public LruCacheWrapper(LruCache<K, V> lruCache) {
        mLruCacheInternal = lruCache;
    }

    /**
     * Get an entry from the cache.
     * @param   key   the key.
     * @return  an entry if there, otherwise null.
     */
    @Nullable
    @Override
    public V get(@NonNull K key) {
        return mLruCacheInternal.get(key);
    }

    /**
     * Put an entry into the cache.
     * @param ref   the import request.
     * @param value     the document to cache.
     */
    @Override
    public void put(@NonNull K ref, @NonNull V value) {
        mLruCacheInternal.put(ref, value);
    }

    /**
     * Default implementation of {@link ComponentCallbacks2}.
     * @param level the memory level.
     */
    @Override
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            mLruCacheInternal.evictAll();
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            mLruCacheInternal.trimToSize(mLruCacheInternal.maxSize() / 2);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // no-op
    }

    @Override
    public void onLowMemory() {
        // no-op
    }
}
