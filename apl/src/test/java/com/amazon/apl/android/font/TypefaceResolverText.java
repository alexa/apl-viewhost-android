/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.graphics.Typeface;

import com.amazon.apl.android.RuntimeConfig;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TypefaceResolverText extends ViewhostRobolectricTest {

    private static final String DEFAULT_FONT_FAMILY = "bookerly";

    @Mock
    private IFontResolver fontResolver;

    @Before
    public void resetTypefaceResolver() {
        TypefaceResolver.getInstance().reset();
    }

    @Test
    public void testDisabledEmbeddedFontLookup() {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder()
                .fontResolver(fontResolver)
                .embeddedFontResolverEnabled(false)
                .build();
        TypefaceResolver.getInstance().initialize(getApplication(), runtimeConfig);

        Typeface face = TypefaceResolver.getInstance().getTypeface(DEFAULT_FONT_FAMILY, 100, false, "", false);

        // This should be a default constructed font.
        Assert.assertEquals(Typeface.SANS_SERIF, face);
    }

    @Test
    public void testEmbeddedFontLookup() {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder()
                .fontResolver(fontResolver)
                .build();
        TypefaceResolver.getInstance().initialize(getApplication(), runtimeConfig);

        Typeface face = TypefaceResolver.getInstance().getTypeface(DEFAULT_FONT_FAMILY, 100, false, "", false);
        Assert.assertNotEquals(Typeface.SANS_SERIF, face);
    }
}
