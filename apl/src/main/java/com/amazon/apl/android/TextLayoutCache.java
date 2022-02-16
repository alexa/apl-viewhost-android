/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;

import com.amazon.apl.android.font.FontConstant;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Dimension;

/**
 * Cache for text layouts (generally {@link StaticLayout}) and also caches
 * {@link TextPaint}). The user is expected to manage creation of the Layout
 * objects and select an appropriate key that makes reuse possible.
 */
public final class TextLayoutCache {
    /**
     * A LruCache of Layouts using the visual hash and some other identifying info as a key.
     */
    private final LruCache<String, Layout> mLayoutCache = new LruCache<>(256);
    /**
     * A LruCache of TextPaint using the visual hash and scaling as a key.
     */
    private final LruCache<String, TextPaint> mPaintCache = new LruCache<>(256);
    /**
     * A LruCache of measured text widths using the visual hash and scaling as a key.
     */
    private final LruCache<String, Integer> mMeasuredTextWidths = new LruCache<>(512);

    /**
     * Writes a layout to the cache
     *
     * @param key A key that makes reuse of layouts possible
     * @param layout The layout that is associated with the key
     */
    public void putLayout(String key, Layout layout) {
        mLayoutCache.put(key, layout);
    }

    /**
     * Gets a cached layout if it exists in the cache or null otherwise.
     *
     * @param key A key that makes reuse of layouts possible
     * @return cached {@link StaticLayout} for a given key.
     */
    @Nullable
    public Layout getLayout(String key) {
        return mLayoutCache.get(key);
    }

    /**
     * Add a width to the text cache.
     *
     * @param key the visual hash
     * @param width the measured width
     */
    public void putTextWidth(String key, int width) {
        mMeasuredTextWidths.put(key, width);
    }

    /**
     * Get a width from the text cache.
     * @param key the visual hash
     * @return the width, or null if not there.
     */
    @Nullable
    public Integer getTextWidth(String key) {
        return mMeasuredTextWidths.get(key);
    }

    /**
     * Gets a cached {@link TextPaint} or creates one if necessary.
     *
     * @return cached {@link TextPaint} if present or creates new one
     */
    @VisibleForTesting
    @NonNull
    public TextPaint getOrCreateTextPaint(int version, String key, TextProxy textProxy, float density) {
        final TextPaint cachedPaint = mPaintCache.get(key);
        if (cachedPaint != null) {
            return cachedPaint;
        }

        TypefaceResolver typefaceResolver = TypefaceResolver.getInstance();
        final Typeface tf = typefaceResolver.getTypeface(textProxy.getFontFamily(),
                textProxy.getFontWeight(), textProxy.isItalic(), textProxy.getFontLanguage(),
                version <= APLVersionCodes.APL_1_0);

        // Create the text paint
        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        if (shouldDisableHinting(textProxy.getFontFamily())) {
            textPaint.setHinting(Paint.HINTING_OFF);
        }

        final int shadowOffsetX = textProxy.getShadowOffsetHorizontal();
        final int shadowOffsetY = textProxy.getShadowOffsetVertical();
        final int shadowColor = textProxy.getShadowColor();
        float shadowBlur = textProxy.getShadowRadius();
        // Avoid shadow layer being removed when shadowBlur is 0
        // https://developer.android.com/reference/android/graphics/Paint.html#setShadowLayer(float,%20float,%20float,%20int)
        if (shadowBlur == 0 && (shadowOffsetX != 0 || shadowOffsetY != 0)) {
            // If this is less than 1 then shadow wont render at all on fos5 devices
            shadowBlur = 1.0f;
        }
        final int fontSize = textProxy.getFontSize().intValue();
        Dimension letterSpacingDim = textProxy.getLetterSpacing();
        final float letterSpacing = letterSpacingDim == null ? 1.0f : letterSpacingDim.value();

        textPaint.setColor(textProxy.getColor());
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(tf);
        // letterSpacing needs to be calculated as EM
        textPaint.setLetterSpacing(letterSpacing / fontSize);
        textPaint.density = density;
        textPaint.setTextScaleX(1.0f);
        textPaint.setShadowLayer(shadowBlur, shadowOffsetX, shadowOffsetY, shadowColor);

        mPaintCache.put(key, textPaint);

        return textPaint;
    }

    /**
     * Clears all caches
     */
    public void clear() {
        mLayoutCache.evictAll();
        mPaintCache.evictAll();
        mMeasuredTextWidths.evictAll();
    }

    private static boolean shouldDisableHinting(String fontFamily) {
        /**
         * Font hinting is a default-on feature that changes the shape of glyphs when
         * rendering at low resolutions according to hints specified in the font definition.
         * This produces odd looking results for amazon-ember-display so we disable it just
         * for that font.
         */
        return FontConstant.fontFamiliesAttachedToAmazonEmber.contains(fontFamily);
    }
}
