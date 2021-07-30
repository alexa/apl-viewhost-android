/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.amazon.apl.android.font.FontConstant;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.text.TextMeasuringInput;

/**
 * Cache for text measurement results (generally {@link StaticLayout} and {@link TextPaint}).
 */
final class TextMeasurementCache implements ITextMeasurementCache {
    private static final String PAINT_KEY_BASE = "PAINT_KEY";
    private static final String PAINT_KEY_PARTS_DELIMITER = ":";

    private final ArrayMap<String, CachedResult> mCache = new ArrayMap<>();
    private final ArrayMap<String, TextPaint> mPaintCache = new ArrayMap<>();

    public void put(String componentId, TextMeasuringInput measuringInput, StaticLayout textLayout) {
        final CachedResult result = new CachedResult();
        result.textLayout = textLayout;
        result.measuringInput = measuringInput;

        mCache.put(componentId, result);
    }

    /**
     * @return cached {@link TextMeasuringInput} for a given componentId.
     */
    @Nullable
    public TextMeasuringInput getMeasuringInput(String componentId) {
        final CachedResult result = mCache.get(componentId);
        if (result == null) {
            return null;
        }
        return result.measuringInput;
    }

    /**
     * @return cached {@link StaticLayout} for a given componentId.
     */
    @Nullable
    public StaticLayout getStaticLayout(String componentId) {
        final CachedResult result = mCache.get(componentId);
        if (result == null) {
            return null;
        }

        return result.textLayout;
    }

    /**
     * @return cached {@link TextPaint} if present. Otherwise creates and caches a new {@link TextPaint}
     * from {@link TextMeasuringInput} for further reuse.
     */
    @NonNull
    public TextPaint getTextPaint(TextMeasuringInput measuringInput) {
        final String key = createKey(measuringInput);
        final TextPaint cachedPaint = mPaintCache.get(key);

        if (cachedPaint != null) {
            return cachedPaint;
        }
        TypefaceResolver typefaceResolver = TypefaceResolver.getInstance();
        final Typeface tf = typefaceResolver.getTypeface(measuringInput.getFontFamily(),
                measuringInput.getFontWeight(), measuringInput.isItalic(), measuringInput.getFontLanguage(), measuringInput.isClassicFonts());

        // Create the text paint
        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        if (shouldDisableHinting(measuringInput.getFontFamily())) {
            textPaint.setHinting(Paint.HINTING_OFF);
        }

        final int shadowOffsetX = measuringInput.getShadowOffsetX();
        final int shadowOffsetY = measuringInput.getShadowOffsetY();
        final int shadowColor = measuringInput.getShadowColor();
        float shadowBlur = measuringInput.getShadowBlur();
        // Avoid shadow layer being removed when shadowBlur is 0
        // https://developer.android.com/reference/android/graphics/Paint.html#setShadowLayer(float,%20float,%20float,%20int)
        if (shadowBlur == 0 && (shadowOffsetX != 0 || shadowOffsetY != 0)) {
            // If this is less than 1 then shadow wont render at all on fos5 devices
            shadowBlur = 1.0f;
        }
        textPaint.setColor(measuringInput.getTextColor());
        textPaint.setTextSize(measuringInput.getFontSize());
        textPaint.setTypeface(tf);
        // letterSpacing needs to be calculated as EM
        textPaint.setLetterSpacing(measuringInput.getLetterSpacing() / measuringInput.getFontSize());
        textPaint.density = measuringInput.getDensity();
        textPaint.setTextScaleX(1.0f);
        textPaint.setShadowLayer(shadowBlur, shadowOffsetX, shadowOffsetY, shadowColor);

        mPaintCache.put(key, textPaint);

        return textPaint;
    }

    private String createKey(TextMeasuringInput measuringInput) {
        final StringBuilder keyBuilder = new StringBuilder(PAINT_KEY_BASE);
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getTextColor());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.isItalic());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getFontFamily());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getFontSize());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getFontWeight());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getLetterSpacing());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getDensity());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getShadowColor());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getShadowBlur());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getShadowOffsetY());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getShadowOffsetX());
        keyBuilder.append(PAINT_KEY_PARTS_DELIMITER);
        keyBuilder.append(measuringInput.getFontLanguage());

        return keyBuilder.toString();
    }

    /**
     * Clears mCache.
     */
    public void clear() {
        mCache.clear();
        mPaintCache.clear();
    }

    private static class CachedResult {
        StaticLayout textLayout;
        TextMeasuringInput measuringInput;
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