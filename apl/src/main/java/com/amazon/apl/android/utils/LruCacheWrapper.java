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
import com.amazon.apl.android.providers.ITelemetryProvider;

/**
 * Simple LruCache that  trims to half when app is about to get killed in BG or
 * system has memory crunch to keep other BH processes running.
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
     * When process is running and not killable then app will receive memory level TRIM_MEMORY_RUNNING_*.
     * App will evict half of the cache when process is running and is not killable but system has critical memory crunch
     * and requires process to release some non-critical resources to prevent system performance degradation.
     * We are not evicting cache for TRIM_MEMORY_RUNNING_LOW and TRIM_MEMORY_RUNNING_MODERATE as these levels
     * doesn't trigger LMKs and recommends clearing only unused resources. As LRUPackage is actively used resource
     * hence it's not cleaned.
     *
     * When app's visibility changes then process will receive level as TRIM_MEMORY_UI_HIDDEN.
     * This doesn't indicate any memory crunch on system hence cache will not be evicted for this level.
     *
     * When app goes to background then it will receive level TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_MODERATE, TRIM_MEMORY_COMPLETE
     * TRIM_MEMORY_BACKGROUND and TRIM_MEMORY_MODERATE indicate that this process is not at high risk of getting
     * killed in background hence not cleaning cache at these memory levels.
     * TRIM_MEMORY_COMPLETE indicate that system is running low on memory and your app is one of the first to be
     * killed if the system does not recover memory now hence we are evicting entire cache at this level to increase
     * chances of keeping process alive for longer duration in background.
     * @param level the memory level.
     */
    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            mLruCacheInternal.trimToSize(mLruCacheInternal.maxSize() / 2);
        } else if (level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            mLruCacheInternal.evictAll();
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

    public int getSize() {
        return mLruCacheInternal.size();
    }
}
