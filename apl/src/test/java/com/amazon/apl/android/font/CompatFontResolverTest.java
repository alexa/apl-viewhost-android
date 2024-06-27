/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompatFontResolverTest extends ViewhostRobolectricTest {

    private CompatFontResolver mCompatFontResolver;

    @Before
    public void setUp() {
        mCompatFontResolver = new CompatFontResolver();
    }

    @Test
    public void testGetMatchingFontFilePath_WithBookerlyFamily() {
        FontKey key1 = FontKey.build("bookerly", 100).italic(true).build();
        String fileName1 = mCompatFontResolver.getMatchingFontFileName(key1);


        FontKey key2  = FontKey.build("Bookerly", 100).italic(true).build();
        String fileName2 = mCompatFontResolver.getMatchingFontFileName(key2);

        // Verify the path
        assertEquals(fileName1, "Bookerly-RegularItalic.ttf");
        assertEquals(fileName2, "Bookerly-RegularItalic.ttf");
    }

    @Test
    public void testGetMatchingFontFilePath_WithAmazonEmberFamily() {
        FontKey key1 = FontKey.build("amazon-ember-display", 100).build();
        String fileName1 = mCompatFontResolver.getMatchingFontFileName(key1);


        FontKey key2 = FontKey.build("amazon_ember", 100).build();
        String fileName2 = mCompatFontResolver.getMatchingFontFileName(key2);

        // Verify the path
        assertEquals(fileName1, "AmazonEmberDisplay_Lt.ttf");
        assertEquals(fileName2, "AmazonEmberDisplay_Lt.ttf");
    }

    @Test
    public void testGetMatchingFontFilePath_withLanguage_correctlyResolved() {
        String[] fontFamilies = { "Bookerly", "Amazon Ember Display" };

        for (String family: fontFamilies) {
            FontKey keyWithLanguage = FontKey.build(family, 400).italic(true).language("en-US").build();
            FontKey keyWithoutLanguage = FontKey.build(family, 400).italic(true).build();
            String fileNameWithLanguage = mCompatFontResolver.getMatchingFontFileName(keyWithLanguage);
            String fileNameWithoutLanguage = mCompatFontResolver.getMatchingFontFileName(keyWithoutLanguage);

            assertNotNull(fileNameWithLanguage);
            assertEquals(fileNameWithLanguage, fileNameWithoutLanguage);
        }
    }
}
