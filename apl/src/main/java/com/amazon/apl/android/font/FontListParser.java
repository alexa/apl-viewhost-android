/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class to get the current font families on an Android device.</p>
 * Code largely taken from  android.graphics.FontListParser
 */
final class FontListParser {

    private static final String TAG = "FontListParser";

    private static final File FONTS_XML = new File("/system/etc/fonts.xml");

    private static final File SYSTEM_FONTS_XML = new File("/system/etc/system_fonts.xml");

    private static List<Family> deviceFonts;

    private static String[][] defaultSystemFonts = {
            {"Amazon Ember", "400", "normal", "Amazon-Ember-Regular.ttf"},
            {"Amazon Ember", "400", "italic", "Amazon-Ember-RegularItalic.ttf"},
            {"Amazon Ember", "700", "normal", "Amazon-Ember-Bold.ttf"},
            {"Amazon Ember", "700", "italic", "Amazon-Ember-BoldItalic.ttf"},
            {"Amazon Ember", "300", "normal", "Amazon-Ember-Light.ttf"},
            {"Amazon Ember", "300", "italic", "Amazon-Ember-LightItalic.ttf"},
            {"Amazon Ember", "100", "normal", "Amazon-Ember-Thin.ttf"},
            {"Amazon Ember", "100", "italic", "Amazon-Ember-ThinItalic.ttf"},
            {"Amazon Ember", "550", "normal", "Amazon-Ember-Medium.ttf"},
            {"Amazon Ember", "550", "italic", "Amazon-Ember-MediumItalic.ttf"}
    };

    /**
     * Map of incorrect file names across various fireos devices
     * to the correct file names.
     */
    @NonNull
    private static final Map<String, String> sInCorrectMap = new ArrayMap<>();

    static {
        sInCorrectMap.put("/system/fonts/Amazon-Ember-Italic.ttf", "/system/fonts/Amazon-Ember-RegularItalic.ttf");
        sInCorrectMap.put("/system/fonts/AmazonEmberDipslay_Bd.ttf", "/system/fonts/AmazonEmberDisplay_Bd.ttf");
    }

    /**
     * Loads the device system fonts. Once loaded, no additional attempts are made on subsequent calls.
     *
     * @return true if the fonts were loaded successfully.
     */
    static synchronized boolean initialize() {
        try {
            if (deviceFonts == null) {
                deviceFonts = getFontFamilies();
            }
            return true;
        } catch (final IOException | XmlPullParserException | RuntimeException e) {
            Log.e(TAG, "Failed to load system fonts", e);
        }
        return false;
    }

    @NonNull
    private static Map<String, String> sAliasMap = new ArrayMap<>();

    static {
        sAliasMap.put("amazon-ember", "Amazon Ember");
    }

    @Nullable
    static synchronized Typeface getMatchingTypeface(final FontKey key) {
        String fontFile = safelyGetMatchingFont(key);

        if (fontFile != null) {
            if (sInCorrectMap.containsKey(fontFile)) {
                Log.e(TAG, "incorrect font file " + fontFile);
                fontFile = sInCorrectMap.get(fontFile);

                if (fontFile == null) {
                    return null;
                }
            }
        }

        try {
            final Typeface font = Typeface.createFromFile(fontFile);
            if (font != null) {
                return font;
            }
        } catch (final RuntimeException e) {
            Log.e(TAG, "FileFontKey file " + fontFile + " not found on the device", e);
        }

        return null;
    }

    /**
     * Parse the font families, filters by fontFamily name and style parameter, returns the closest
     * absolute weight available.
     *
     * @param key The font family, weight and italicisation.
     * @return the location of the font file that best matches the search parameters or null.
     */
    @Nullable
    private static String getMatchingFont(final FontKey key) {
        if (key == null || key.getFamily() == null) {
            return null;
        }

        for (Family family : deviceFonts) {
            if (key.getFamily().equalsIgnoreCase(family.name) || key.getFamily().equalsIgnoreCase(sAliasMap.get(family.name))) {
                return filter(family.fonts, key.getWeight(), key.isItalic());
            }
            if (family.lang != null) {
                String langCode = key.getLanguage().split("-")[0];
                if (langCode.equalsIgnoreCase(family.lang)) {
                    return filter(family.fonts, key.getWeight(), key.isItalic());
                }
            }
        }
        return null;
    }

    /**
     * Filters and returns the closest font in the font family given the desired font weight
     * and whether the style is italic or not.
     *
     * @param fontList list of font list.
     * @param weight   weight of the font desired
     * @param isItalic whether or not the font style is italics
     * @return the location of the font file that best matches the search parameters or null.
     */
    @Nullable
    private static String filter(final List<FileFontKey> fontList, final int weight, final boolean isItalic) {
        String bestMatch = null;
        List<FileFontKey> matches = new ArrayList<>();
        for (FileFontKey font : fontList) {
            if (font.italic == isItalic) {
                matches.add(font);
            }
        }

        // Fallback to regular system fonts (instead of returning null and resolving with embedded fonts.
        // if a font-family in fonts.xml does not contain any italic variation.
        if (matches.size() == 0) {
            matches = fontList;
        }

        if (matches.size() != 0) {
            //return the best match based on weight.
            int minDiff = Integer.MAX_VALUE;
            for (FileFontKey match : matches) {
                if (minDiff > Math.abs(match.weight - weight)) {
                    bestMatch = match.fileName;
                    minDiff = Math.abs(match.weight - weight);
                }
            }
        }

        return bestMatch;
    }

    @NonNull
    private static List<Family> getFontFamilies() throws IOException, XmlPullParserException {
        String fontsXml;
        if (FONTS_XML.exists()) {
            fontsXml = FONTS_XML.getAbsolutePath();
        } else if (SYSTEM_FONTS_XML.exists()) {
            fontsXml = SYSTEM_FONTS_XML.getAbsolutePath();
        } else {
            throw new RuntimeException("fonts.xml does not exist on this system");
        }
        Config parser = parse(new FileInputStream(fontsXml));

        if (parser.families == null) {
            throw new IllegalStateException("Parsing returned no fonts!");
        }
        return parser.families;
    }

    /**
     * Finds the closest font file
     *
     * @param key The font family, weight and italicisation.
     * @return the location of ttf file representing the closest font or loads the closest
     * Amazon Ember font given the font weight &  style.
     */
    @Nullable
    private static String safelyGetMatchingFont(final FontKey key) {
        try {
            return getMatchingFont(key);
        } catch (final RuntimeException e) {
            Log.e(TAG, "Could not load /system/etc/fonts.xml file - defaulting to standard file");
            List<FileFontKey> fonts = new ArrayList<>();
            for (String[] names : defaultSystemFonts) {
                File file = new File("/system/fonts/", names[3]);
                if (file.exists()) {
                    fonts.add(new FileFontKey(Integer.parseInt(names[1]), names[2].equalsIgnoreCase("italic"), names[3]));
                }
            }
            return filter(fonts, key.getWeight(), key.isItalic());
        }
    }

    /* Parse fallback list (no names) */
    @NonNull
    private static Config parse(@NonNull final InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, null);
            parser.nextTag();
            return readFamilies(parser);
        } finally {
            in.close();
        }
    }

    @NonNull
    private static Alias readAlias(XmlPullParser parser) throws XmlPullParserException, IOException {
        Alias alias = new Alias();
        alias.name = parser.getAttributeValue(null, "name");
        alias.toName = parser.getAttributeValue(null, "to");
        String weightStr = parser.getAttributeValue(null, "mWeight");
        if (weightStr == null) {
            alias.weight = 0;
        } else {
            alias.weight = Integer.parseInt(weightStr);
        }
        skip(parser); // alias tag is empty, ignore any contents and consume end tag
        return alias;
    }

    /**
     * Reads the familyset tag in the font xml file.
     */
    @NonNull
    private static Config readFamilies(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        Config config = new Config();
        parser.require(XmlPullParser.START_TAG, null, "familyset");
        while (parser.next() != XmlPullParser.END_TAG) {
            try {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (parser.getName().equals("family")) {
                    config.families.add(readFamily(parser));
                } else if (parser.getName().equals("alias")) {
                    config.aliases.add(readAlias(parser));
                } else {
                    skip(parser);
                }
            } catch (XmlPullParserException ex) {
                Log.wtf(TAG, "System fonts.xml could not be parsed properly");
                return new Config();
            }
        }
        return config;
    }

    /**
     * Reads individual family tag
     */
    private static Family readFamily(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        String name = parser.getAttributeValue(null, "name");
        String lang = parser.getAttributeValue(null, "lang");
        String variant = parser.getAttributeValue(null, "variant");
        List<FileFontKey> fonts = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            if (tag.equals("font")) {
                String weightStr = parser.getAttributeValue(null, "weight");
                int weight = weightStr == null ? 400 : Integer.parseInt(weightStr);
                boolean isItalic = "italic".equals(parser.getAttributeValue(null, "style"));
                if(parser.next() == XmlPullParser.TEXT) {
                    String filename = parser.getText();
                    String fullFilename = "/system/fonts/" + filename;
                    fonts.add(new FileFontKey(weight, isItalic, fullFilename));
                    // Keep iterating to next parser, till we get the end font tag to handle fonts with axis tag on FOS 7 fonts.xml
                    while(parser.getEventType() != XmlPullParser.END_TAG || !parser.getName().equals("font")) {
                        parser.next();
                    }
                }
            } else {
                skip(parser);
            }
        }
        return new Family(name, fonts, lang, variant);
    }

    private static void skip(@NonNull final XmlPullParser parser) throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
            }
        }
    }

    private FontListParser() {

    }

    /**
     * Font alias
     */
    public static class Alias {

        public String name;

        String toName;

        int weight;
    }

    public static class Config {

        final List<Alias> aliases;

        final List<Family> families;

        Config() {
            families = new ArrayList<>();
            aliases = new ArrayList<>();
        }

    }

    /**
     * FontFamily class represents a list of fonts within the same font family.
     */
    static class Family {
        final List<FileFontKey> fonts;
        final public String lang;
        @Nullable
        final public String name;
        final String variant;

        Family(
                @Nullable final String name,
                final List<FileFontKey> fonts,
                final String lang,
                final String variant) {
            this.name = name;
            this.fonts = fonts;
            this.lang = lang;
            this.variant = variant;
        }
    }

    /**
     * Font class represents a Android Font class.
     */
    static class FileFontKey {

        final String fileName;      //the location of the font ttf file.
        final int weight;
        final boolean italic;

        FileFontKey(int weight, boolean isItalic,
                    String fontFileName) {
            this.weight = weight;
            this.italic = isItalic;
            this.fileName = fontFileName;
        }
    }
}
