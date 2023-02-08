/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class CompatFontResolverParametrizedTest {

    private final String family;
    private final int weight;
    private final boolean italic;
    private final String language;
    private final String expectedResult;

    private CompatFontResolver mCompatFontResolver;

    @Parameterized.Parameters(name = "{index}: {0} {1} italic:{2} lang:{3} == {4}")
    public static Collection<Object[]> fontFamilies() {
        return Arrays.asList(new Object[][] {
                { "Bookerly", 100, false, null,             "Bookerly-Regular.ttf" },
                { "Bookerly", 100, false, "en-US",          "Bookerly-Regular.ttf"},
                { "Bookerly", 100, false, "en-CA",          "Bookerly-Regular.ttf"},
                { "bookerly", 100, true, null,              "Bookerly-RegularItalic.ttf"},
                { "bookerly", 900, true, null,              "Bookerly-BoldItalic.ttf"},
                { "amazon-ember-display", 900, true, null,  "Amazon-Ember-BoldItalic.ttf"},
                { "Amazon Ember Display", 400, false, null, "AmazonEmberDisplay_Rg.ttf"},
                { "amazon_ember", 700, false, null,         "AmazonEmberDisplay_Bd.ttf"},
                { "sans-serif", 400, false, "en-US",        "AmazonEmberDisplay_Rg.ttf"},
                { "sans-serif", 400, false, "fr-FR",        "AmazonEmberDisplay_Rg.ttf"},
                { "nonexistent", 123, true, "aa-ZZ",        null},
                { "nonexistent", 0, false, null,            null},
        });
    }

    public CompatFontResolverParametrizedTest(String family, int weight, boolean italic, String language, String expectedResult) {
        this.family = family;
        this.weight = weight;
        this.italic = italic;
        this.language = language;
        this.expectedResult = expectedResult;
    }
    @Before
    public void setUp() {
        mCompatFontResolver = new CompatFontResolver();
    }

    @Test
    public void test() {
        FontKey.Builder builder = FontKey.build(family, weight).italic(italic);
         if (language != null) {
             builder.language(language);
         }
        FontKey key = builder.build();
        String fileName = mCompatFontResolver.getMatchingFontFileName(key);

        Assert.assertEquals(expectedResult, fileName);

    }
}
