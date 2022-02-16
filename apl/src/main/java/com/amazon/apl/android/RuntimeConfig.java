/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.bitmap.NoOpBitmapCache;
import com.amazon.apl.android.bitmap.NoOpBitmapPool;
import com.amazon.apl.android.dependencies.IPackageCache;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.font.IFontResolver;
import com.google.auto.value.AutoValue;

/*
 *  Runtime Configuration
 */
@AutoValue
public abstract class RuntimeConfig {
    // Runtime Font Resolver
    public abstract IFontResolver getFontResolver();

    public abstract boolean isPreloadingFontsEnabled();

    public abstract IBitmapPool getBitmapPool();

    public abstract IBitmapCache getBitmapCache();

    @Nullable
    public abstract IPackageCache getPackageCache();

    /**
     * Whether or not we should clear the views when a document is finished. Currently used
     * for transparent activity case with Video components as clearing the layout causes the
     * home screen to show through.
     *
     * @return true if we need to clear views when we finish a document
     */
    public abstract boolean isClearViewsOnFinish();

    /**
     * @return gets a builder for {@link RuntimeConfig} with default implementation
     */
    public static Builder builder() {
        IBitmapCache bitmapCache = new NoOpBitmapCache();
        IBitmapPool bitmapPool = new NoOpBitmapPool();
        return new AutoValue_RuntimeConfig.Builder()
                .fontResolver(new CompatFontResolver())
                .preloadingFontsEnabled(true)
                .bitmapCache(bitmapCache)
                .bitmapPool(bitmapPool)
                .clearViewsOnFinish(true);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Defaults to {@link CompatFontResolver}
         * @param fontResolver runtime font resolver
         * @return this builder
         */
        public abstract Builder fontResolver(@NonNull IFontResolver fontResolver);

        /**
         * Defaults to true
         * @param shouldPreloadFonts specifies whether the viewhost should preload common fonts
         * @return this builder
         */
        public abstract Builder preloadingFontsEnabled(boolean shouldPreloadFonts);

        /**
         * Bitmap pool to use for creating new Bitmap objects.
         *
         * @param bitmapPool bitmap pool
         * @return this builder
         */
        public abstract Builder bitmapPool(IBitmapPool bitmapPool);

        /**
         * Bitmap cache to use (key-value store).
         *
         * @param bitmapCache bitmap cache
         * @return this builder
         */
        public abstract Builder bitmapCache(IBitmapCache bitmapCache);


        /**
         * A memory cache for import requests
         * @param packageCache import request cache.
         * @return this builder
         */
        public abstract Builder packageCache(@NonNull IPackageCache packageCache);

        /**
         * Defaults to true.
         * Flag to clear android views when we finish the apl document.
         * @param clearViewsOnFinish true if views should be cleared on finish
         * @return this builder
         */
        public abstract Builder clearViewsOnFinish(boolean clearViewsOnFinish);

        /**
         * Builds the config
         * @return the {@link RuntimeConfig}
         */
        public abstract RuntimeConfig build();
    }
}
