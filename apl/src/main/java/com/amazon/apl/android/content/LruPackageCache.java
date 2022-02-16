/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.dependencies.IPackageCache;
import com.amazon.apl.android.utils.LruCacheWrapper;

/**
 * An LruCache cache for {@link com.amazon.apl.android.Content.ImportRequest}.
 */
public class LruPackageCache extends LruCacheWrapper<Content.ImportRef, APLJSONData> implements IPackageCache {
    private static final int MEMORY_ALLOCATION = 2 * 1024 * 1024; // 2 MiB

    public LruPackageCache() {
        this(MEMORY_ALLOCATION);
    }

    public LruPackageCache(int sizeInBytes) {
        super(new LruCache<Content.ImportRef, APLJSONData>(sizeInBytes) {
            @Override
            protected int sizeOf(@NonNull Content.ImportRef key, @NonNull APLJSONData value) {
                // Size is number of characters. Each character should be two bytes.
                return value.getSize() * 2;
            }
        });
    }
}
