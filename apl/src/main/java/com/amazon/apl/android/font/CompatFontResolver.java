/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Default Font resolver used for all sdk.  This class has burden of reading system
 * font files for non Amazon fonts.
 */
public class CompatFontResolver implements IFontResolver {

    private static final String TAG = "CompatFontResolver";

    @Override
    @Nullable
    public Typeface findFont(@NonNull final FontKey key) {
        return getMatchingFont(key);
    }

    /**
     * Finds the closest font file. We start by using the fonts.xml file and trying to find a good match.
     * If that fails, we let Android pick a font.
     *
     * @param key   The font to load.
     * @return the location of ttf file representing the closest font or null.
     */
    @Nullable
    static synchronized Typeface getMatchingFont(final FontKey key) {
        try {
            Typeface typeface = FontListParser.getMatchingTypeface(key);
            return typeface;
        } catch (RuntimeException e) {
            Log.e(TAG, "FontKey file " + key.getFamily() + " not found on the device", e);
        }
        return null;
    }

    @Override
    public void initialize() {
        // heavy initialization
        boolean result = FontListParser.initialize();

        if (!result) {
            Log.e(TAG, "Failed to load system fonts");
        }
    }
}