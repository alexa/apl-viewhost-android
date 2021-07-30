/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.RuntimeConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The ViewHost internal typeface resolver API.
 * Responsible for obtaining font {@link Typeface} information.
 */
public class TypefaceResolver {
    private IFontResolver mRuntimeFontResolver;
    private static final TypefaceResolver sInstance = new TypefaceResolver();
    private static final String TAG = "TypefaceResolver";
    private static final Typeface DEFAULT_TYPEFACE = Typeface.SANS_SERIF;
    private Future<Boolean> mInitializeResolvers;
    static final LruCache<FontKey, Typeface> FONT_CACHE = new LruCache<>(150);

    private enum  InitializationState {
        UNINITIALIZED,
        INITIALIZING,
        INITIALIZED
    }

    private InitializationState mInitializationState = InitializationState.UNINITIALIZED;

    /**
     * Private Constructor for singleton */
    private TypefaceResolver() {
    }

    /**
     * @return Single instance of the TypefaceResolver
     */
    public static TypefaceResolver getInstance() {
        return TypefaceResolver.sInstance;
    }

    /**
     * @param context The Android context.
     * @param runtimeConfig allows the runtime to configure the typeface resolver
     */
    public synchronized void initialize(Context context, RuntimeConfig runtimeConfig) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Initializing Amazon Fonts");
        }
        if (runtimeConfig.getFontResolver() == null) {
            return;
        }
        if (mInitializationState == InitializationState.UNINITIALIZED) {
            // set state to initializing
            mInitializationState = InitializationState.INITIALIZING;
            mRuntimeFontResolver = runtimeConfig.getFontResolver();
            mInitializeResolvers = FontUtil.SEQUENTIAL_EXECUTOR.submit(new InitializeResolvers(runtimeConfig.isPreloadingFontsEnabled()));
        }
    }

    private boolean waitUntilFontsAvailable()  {
        try {
            return mInitializeResolvers.get();
        } catch (final ExecutionException ex) {
            Log.e(TAG, "System fonts failed to load", ex);
        } catch (InterruptedException ex) {
            Log.e(TAG, "System fonts loading interrupted", ex);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private class InitializeResolvers implements Callable<Boolean> {
        private final boolean mShouldPreloadFonts;

        private InitializeResolvers(final boolean shouldPreloadFonts) {
            mShouldPreloadFonts = shouldPreloadFonts;
        }

        @Override
        public Boolean call() {
            // call resolver heavy initializers
            mRuntimeFontResolver.initialize();

            // cache commonly used Amazon system fonts
            if (mShouldPreloadFonts) {
                preloadFonts();
            }

            mInitializationState = InitializationState.INITIALIZED;
            return true;
        }
    }

    /* Gets a IFontResolver instance.
     * @return Returns a stored instance of the Runtime resolver
     */
    private IFontResolver getRuntimeResolver() {
        return mRuntimeFontResolver;
    }

    /**
     * Gets a FileFontKey based on font-family, fontWeight, italic.
     *
     * @param fontFamily    The name of the font family, sans-serif used if blank or empty string.
     * @param fontWeight    The weight of the font.
     * @param italic        The italic styling of the font.
     * @param isAPLOneZero  Is the requesting document in APL 1.0.
     * @return A font matching the specification, or the system's best guess.
     */
    @NonNull
    public Typeface getTypeface(
            final String fontFamily,
            final int fontWeight,
            final boolean italic,
            final String language,
            final boolean isAPLOneZero) {

        if(waitUntilFontsAvailable()) {
            try {
                FontKey fontKey = FontKey.build(fontFamily, fontWeight).italic(italic).language(language).build();
                final Typeface cachedFont = TypefaceResolver.FONT_CACHE.get(fontKey);

                if (cachedFont != null) {
                    return cachedFont;
                }
                Typeface typeface;

                typeface = getTypefaceFromFontFamily(fontKey, getRuntimeResolver());
                if (typeface == null) {
                    typeface = Typeface.create(
                            isAPLOneZero ? null : DEFAULT_TYPEFACE,
                            italic ? Typeface.ITALIC : Typeface.NORMAL);
                }

                if (typeface != null) {
                    TypefaceResolver.FONT_CACHE.put(fontKey, typeface);
                    return typeface;
                }
            } catch (final RuntimeException e) {
                Log.e(TAG, "FileFontKey file " + fontFamily + " not found on the device", e);
            }

        }

        return null;
    }

    @Nullable
    private Typeface getTypefaceFromFontFamily(FontKey key, IFontResolver fontResolver) {
        Typeface result = null;
        String fontFamily = key.getFamily();
        if (fontFamily.contains(",")) {
            for (String font : fontFamily.split(",")) {
                if (!font.isEmpty()) {
                    FontKey fontKey = FontKey.build(font.trim(), key.getWeight()).italic(key.isItalic()).language(key.getLanguage()).build();
                    final Typeface cachedFont = TypefaceResolver.FONT_CACHE.get(fontKey);

                    if (cachedFont != null) {
                        return cachedFont;
                    }

                    result = fontResolver.findFont(fontKey);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }
        return fontResolver.findFont(key);
    }

    /**
     * Preload commonly used fonts
     */
    private synchronized void preloadFonts() {
        List<FontKey> commonFontKeys = getCommonlyUsedFontKeys();
        for(FontKey fontKey: commonFontKeys) {
            FontUtil.SEQUENTIAL_EXECUTOR.execute(() -> getTypeface(fontKey.getFamily(), fontKey.getWeight(), fontKey.isItalic(), fontKey.getLanguage(), false));
        }
    }

    /**
     * Explicitly add a font to the cache for the defined key. Use this if your resolver knows that
     * multiple FontKeys will map to the same Typeface object.
     * @param fontKey   the fontkey
     * @param typeface  the typeface
     */
    public static void putTypeface(FontKey fontKey, Typeface typeface) {
        FONT_CACHE.put(fontKey, typeface);
    }

    /**
     * Gets a list of commonly used font to warm up the fonts Cache.
    */
    private ArrayList<FontKey> getCommonlyUsedFontKeys() {
        ArrayList<FontKey> commonFontKeys = new ArrayList<>();
        String[] fontFamilies = {"amazon-ember-display", "bookerly"};
        for(String family : fontFamilies) {
            for (int i = 100; i <= 900; i+=100) {
                commonFontKeys.add(FontKey.build(family, i).build());
                commonFontKeys.add(FontKey.build(family, i).italic(true).build());
            }
        }
        return commonFontKeys;
    }
}
