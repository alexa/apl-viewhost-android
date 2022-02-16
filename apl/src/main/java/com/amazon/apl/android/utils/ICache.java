/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.content.ComponentCallbacks2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple cache interface that extends {@link ComponentCallbacks2}.
 *
 * @param <K> the keys
 * @param <V> the values
 */
public interface ICache<K,V> extends ComponentCallbacks2 {
    /**
     * Get a value from the cache, null if not present.
     * @param key the key
     * @return the value or null if not present.
     */
    @Nullable
    V get(@NonNull K key);

    /**
     * Put a value into the cache.
     * @param key   the key
     * @param val   the value
     */
    void put(@NonNull K key, @NonNull V val);
}
