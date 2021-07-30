/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.graphics.Paint;

import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.font.FontConstant;
import com.amazon.apl.android.font.IFontResolver;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.text.TextMeasuringInput;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

public class TextMeasurementCacheTest extends ViewhostRobolectricTest {

    @Mock
    private Context context;
    @Mock
    private IFontResolver fontResolver;

    private ITextMeasurementCache textMeasurementCache;

    @Before
    public void setUp() {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder().fontResolver(fontResolver).build();
        TypefaceResolver.getInstance().initialize(context, runtimeConfig);
        textMeasurementCache = new TextMeasurementCache();
    }

    @Test
    public void testHintingDisabledForEmberFontFamilies() {
        for (String fontFamily: FontConstant.fontFamiliesAttachedToAmazonEmber) {
            TextMeasuringInput textMeasuringInput = new TextMeasuringInput.Builder()
                    .fontFamily(fontFamily)
                    .build();

            Paint paint = textMeasurementCache.getTextPaint(textMeasuringInput);

            assertEquals(Paint.HINTING_OFF, paint.getHinting());
        }
    }
}
