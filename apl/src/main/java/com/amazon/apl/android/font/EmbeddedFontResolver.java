/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.FontRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.amazon.apl.android.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Fallback Font resolver - that uses embedded fonts.
 */
public class EmbeddedFontResolver implements IFontResolver {

    private final Context mAppContext;
    private static final String TAG = "EmbeddedFontResolver";

    private static final List<ResFontKey> sArabicFontWeights = Collections.unmodifiableList(Arrays.asList(
            new ResFontKey(100, R.font.amazon_ember_regular_v2),
            new ResFontKey(300, R.font.amazon_ember_regular_v2),
            new ResFontKey(400, R.font.amazon_ember_regular_v2),
            new ResFontKey(500, R.font.amazon_ember_medium_v2),
            new ResFontKey(700, R.font.amazon_ember_bold_v2),
            new ResFontKey(900, R.font.amazon_ember_bold_v2)));

    private static final List<ResFontKey> sFontWeights = Collections.unmodifiableList(Arrays.asList(
            new ResFontKey(100, R.font.amazon_ember_display_light),
            new ResFontKey(300, R.font.amazon_ember_display_light),
            new ResFontKey(400, R.font.amazon_ember_display_regular),
            new ResFontKey(500, R.font.amazon_ember_display_medium),
            new ResFontKey(700, R.font.amazon_ember_display_bold),
            new ResFontKey(900, R.font.amazon_ember_display_heavy)));

    private static class ResFontKey {
        @FontRes final int fontRes;
        final int weight;

        ResFontKey(final int weight, @FontRes final int fontRes) {
            this.weight = weight;
            this.fontRes = fontRes;
        }
    }

    public EmbeddedFontResolver(@NonNull final Context context) {
        mAppContext = context.getApplicationContext();
    }


    @NonNull
    @Override
    public Typeface findFont(@NonNull FontKey key) {
        Typeface result = null;
        try {
            result = findAPLFont(key);
        } catch (final Resources.NotFoundException ex) {
            Log.e(TAG, "Exception finding embedded app font", ex);
        }
        if (result == null) {
            Log.w(TAG, "Did not find font with key: " + key);
        }
        return result;
    }

    @Override
    public void initialize() {
        // No-op since this implementation is mostly a fall-back. Fonts should be loaded from
        //  system/fonts not from our resources.
    }

    @Nullable
    private Typeface findAPLFont(@NonNull final FontKey key) {
        // Get the closest APLFont font
        int minDiff = Integer.MAX_VALUE;
        ResFontKey bestKey = null;
        List<ResFontKey> fontWeights = sFontWeights;

        boolean isArabicFontKey = FontUtil.isArabicFontKey(key);
        if (isArabicFontKey) {
            fontWeights = sArabicFontWeights;
        }

        for (final ResFontKey currentKey : fontWeights) {
            final int currentWeight = currentKey.weight;

            if (minDiff > Math.abs(currentWeight - key.getWeight())) {
                minDiff = Math.abs(currentWeight - key.getWeight());
                bestKey = currentKey;
            }
        }

        if (bestKey != null) {
            final Typeface result = ResourcesCompat.getFont(mAppContext, bestKey.fontRes);
            if (key.isItalic() && !isArabicFontKey) {
                return Typeface.create(result, Typeface.ITALIC);
            }
            return result;
        }
        return null;
    }
}




