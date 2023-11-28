/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;

import com.amazon.apl.android.Component;
import com.amazon.common.storage.WeakCache;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Uses {@link WeakCache} to store weak references of bitmaps in memory
 * Also maintains a list of {@link WeakReference} to {@link Component} objects
 * which provides for linear look-up of components with shadow bitmaps
 */
public class ShadowCache {
    private final WeakCache<BitmapKey, Bitmap> mCache = new WeakCache<>();
    private final List<WeakReference<Component>> mComponents = new ArrayList<>();

    public void putShadow(BitmapKey key, Component component) {
        mCache.put(key, new WeakReference<>(component.getShadowBitmap()));
        mComponents.add(new WeakReference<>(component));
    }

    public Iterator<WeakReference<Component>> getComponents() {
        return mComponents.iterator();
    }

    public Bitmap getShadow(BitmapKey key) {
        return mCache.get(key);
    }

    public void clear() {
        mCache.clear();
        mComponents.clear();
    }
}
