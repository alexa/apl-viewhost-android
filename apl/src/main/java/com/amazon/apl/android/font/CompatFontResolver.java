/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default Font resolver used for all sdk.  This class has burden of reading system
 * font files for non Amazon fonts.
 */
public class CompatFontResolver implements IFontResolver {

    private static final String TAG = "CompatFontResolver";
    private static final String AMAZON_EMBER_DISPLAY = "amazon-ember-display";
    private static final String BOOKERLY_FAMILY = "bookerly";

    private static final String FONT_DIRECTORY = "/system/fonts/";
    @NonNull
    private static Map<String, Map<FontKey, String>> sFontToFile = createFontToFileMap();
    private static Map<String, Set<FontKey>> sFileToFont = createFileToFontMap(sFontToFile);

    @Override
    @Nullable
    public Typeface findFont(@NonNull final FontKey key) {
        //use sFontToFile hardcoded font to file mapping to handle misnamed fonts and cache most commonly used fonts
        if (FontConstant.fontFamiliesAttachedToAmazonEmber.contains(key.getFamily()) || FontConstant.fontFamiliesAttachedToBookerly.contains(key.getFamily())) {
            String fileName = getMatchingFontFileName(key);
            Typeface typeface = getMatchingFontFromFile(FONT_DIRECTORY + fileName);
            if (typeface != null) {
                putAllTypefaces(fileName, typeface);
            }
            return typeface;
        }
        return getMatchingFont(key);
    }

    private static void putAllTypefaces(@NonNull String fileName, @NonNull Typeface typeface) {
        Set<FontKey> fontKeysWithSameTypeface = sFileToFont.get(fileName);
        if (fontKeysWithSameTypeface != null) {
            for (FontKey fontKey : fontKeysWithSameTypeface) {
                TypefaceResolver.putTypeface(fontKey, typeface);
            }
        }
    }

    /**
     * @return the matching file for Amazon Ember / Bookerly font resources
     */
    @VisibleForTesting
    public String getMatchingFontFileName(FontKey key) {
        String fileName;
        if (FontConstant.fontFamiliesAttachedToAmazonEmber.contains(key.getFamily())) {
            Map<FontKey, String> fontFamiliesAttachedToAmazonEmber = sFontToFile.get(AMAZON_EMBER_DISPLAY);
            fileName = lookUpFontFileName(AMAZON_EMBER_DISPLAY, key, fontFamiliesAttachedToAmazonEmber);
        } else {
            Map<FontKey, String> fontFamiliesAttachedToBookerly = sFontToFile.get(BOOKERLY_FAMILY);
            fileName = lookUpFontFileName(BOOKERLY_FAMILY, key, fontFamiliesAttachedToBookerly);
        }

        return fileName;
    }

    private String lookUpFontFileName(String fontFamily, FontKey key, Map<FontKey, String> fontFilenames) {
        FontKey lookUpKey = FontKey.build(fontFamily, key.getWeight()).italic(key.isItalic()).language(key.getLanguage()).build();
        String filename = fontFilenames.get(lookUpKey);
        if (filename == null && ! lookUpKey.getLanguage().isEmpty()) {
            // try without language
            lookUpKey = FontKey.build(fontFamily, key.getWeight()).italic(key.isItalic()).build();
            filename = fontFilenames.get(lookUpKey);
        }
        return filename;
    }

    @Nullable
    private static Typeface getMatchingFontFromFile(final String path) {
        final File file = new File(path);

        if (file.exists()) {
            return Typeface.createFromFile(path);
        }
        return null;
    }

    /**
     * Finds the closest font file. We start by using a hardcoded list.  If that fails, we fall
     * back to using the fonts.xml file and trying to find a good match.  If that fails, we
     * let Android pick a font.
     *
     * @param key   The font to load.
     * @return the location of ttf file representing the closest font or null.
     * Amazon Ember font given the font weight &  style.
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

    private static Map<String, Map<FontKey, String>> createFontToFileMap() {
        final Map<String, Map<FontKey, String>> result = new ArrayMap<>();

        // Amazon Ember font resources
        String family = AMAZON_EMBER_DISPLAY;
        final Map<FontKey, String> AMAZON_EMBER = new ArrayMap<>();


        AMAZON_EMBER.put(FontKey.build(family, 100).build(), "AmazonEmberDisplay_Lt.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 200).build(), "AmazonEmberDisplay_Lt.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 300).build(), "AmazonEmberDisplay_Lt.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 400).build(), "AmazonEmberDisplay_Rg.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 500).build(), "AmazonEmberDisplay_Md.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 600).build(), "AmazonEmberDisplay_Md.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 700).build(), "AmazonEmberDisplay_Bd.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 800).build(), "AmazonEmberDisplay_Bd.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 900).build(), "AmazonEmberDisplay_He.ttf");

        AMAZON_EMBER.put(FontKey.build(family, 100).italic(true).build(), "Amazon-Ember-ThinItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 200).italic(true).build(), "Amazon-Ember-ThinItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 300).italic(true).build(), "Amazon-Ember-LightItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 400).italic(true).build(), "Amazon-Ember-RegularItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 500).italic(true).build(), "Amazon-Ember-MediumItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 600).italic(true).build(), "Amazon-Ember-MediumItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 700).italic(true).build(), "Amazon-Ember-BoldItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 800).italic(true).build(), "Amazon-Ember-BoldItalic.ttf");
        AMAZON_EMBER.put(FontKey.build(family, 900).italic(true).build(), "Amazon-Ember-BoldItalic.ttf");

        result.put(AMAZON_EMBER_DISPLAY, AMAZON_EMBER);

        // Bookerly font resources
        family = BOOKERLY_FAMILY;
        final Map<FontKey, String> BOOKERLY = new ArrayMap<>();

        BOOKERLY.put(FontKey.build(family, 100).build(), "Bookerly-Regular.ttf");
        BOOKERLY.put(FontKey.build(family, 200).build(), "Bookerly-Regular.ttf");
        BOOKERLY.put(FontKey.build(family, 300).build(), "Bookerly-Regular.ttf");
        BOOKERLY.put(FontKey.build(family, 400).build(), "Bookerly-Regular.ttf");
        BOOKERLY.put(FontKey.build(family, 500).build(), "Bookerly-Bold.ttf");
        BOOKERLY.put(FontKey.build(family, 600).build(), "Bookerly-Bold.ttf");
        BOOKERLY.put(FontKey.build(family, 700).build(), "Bookerly-Bold.ttf");
        BOOKERLY.put(FontKey.build(family, 800).build(), "Bookerly-Bold.ttf");
        BOOKERLY.put(FontKey.build(family, 900).build(), "Bookerly-Bold.ttf");

        BOOKERLY.put(FontKey.build(family, 100).italic(true).build(), "Bookerly-RegularItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 200).italic(true).build(), "Bookerly-RegularItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 300).italic(true).build(), "Bookerly-RegularItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 400).italic(true).build(), "Bookerly-RegularItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 500).italic(true).build(), "Bookerly-BoldItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 600).italic(true).build(), "Bookerly-BoldItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 700).italic(true).build(), "Bookerly-BoldItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 800).italic(true).build(), "Bookerly-BoldItalic.ttf");
        BOOKERLY.put(FontKey.build(family, 900).italic(true).build(), "Bookerly-BoldItalic.ttf");

        result.put(BOOKERLY_FAMILY, BOOKERLY);

        return result;
    }

    private static Map<String, Set<FontKey>> createFileToFontMap(Map<String, Map<FontKey, String>> fontToFile) {
        final Map<String, Set<FontKey>> result = new ArrayMap<>();
        for (Map<FontKey, String> fontMap : fontToFile.values()) {
            for (Map.Entry<FontKey, String> entry : fontMap.entrySet()) {
                Set<FontKey> fontKeys = result.get(entry.getValue());
                if (fontKeys == null) {
                    fontKeys = new HashSet<>();
                    result.put(entry.getValue(), fontKeys);
                }
                fontKeys.add(entry.getKey());
            }
        }

        return result;
    }
}
