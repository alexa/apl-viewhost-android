/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.common.storage;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A generic Weak Cache to store weak references in memory.
 * Users of this cache must store a strong reference to the Value object elsewhere.
 * @param <K>
 * @param <V>
 */
public class WeakCache<K, V> {
    private final Map<K, WeakReference<V>> mCache = new HashMap<>();

    /**
     * Insert a weak reference to the cache
     * The underlying data structure is swept off any invalid weak references on every call.
     * @param key
     * @param weakReference
     */
    public void put(K key, WeakReference<V> weakReference) {
        sweepCache();
        mCache.put(key, weakReference);
    }

    /**
     * Get a value by key
     * @param key
     * @return the strong reference or null if not found or valid.
     */
    @Nullable
    public V get(K key) {
        WeakReference<V> weakValue = mCache.get(key);
        if (weakValue != null) {
            V value = weakValue.get();
            if (value != null) {
                return value;
            }
            mCache.remove(key);
        }
        return null;
    }

    /**
     * Remove entry by key
     * @param key
     */
    public void remove(K key) {
        mCache.remove(key);
    }

    /**
     * Clear the cache
     */
    public void clear() {
        mCache.clear();
    }

    /**
     * Get the number of entries in the cache
     * @return the number of entries
     */
    public int size() {
        return mCache.size();
    }

    /**
     * Method to sweep the cache off any invalid weak references
     */
    private void sweepCache() {
        // Remove entries which do not have a strong reference
        Iterator<Map.Entry<K, WeakReference<V>>> iter = mCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<K, WeakReference<V>> entry = iter.next();
            if (entry.getValue() != null && entry.getValue().get() == null) {
                iter.remove();
            }
        }
    }
}
