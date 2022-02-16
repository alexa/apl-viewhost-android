/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.font.FontConstant;
import com.amazon.apl.android.font.IFontResolver;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.FontStyle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TextLayoutCacheTest extends ViewhostRobolectricTest {

    private static final String DEFAULT_FONT_FAMILY = "sans-serif";
    private static final String TEXT_FONT_LANGUAGE = "ja-JP";

    @Mock
    private Context context;
    @Mock
    private IFontResolver fontResolver;
    @Mock
    private TextProxy mTextProxy;
    @Mock
    private Layout mLayout;
    @Mock
    private IBitmapPool mBitmapPool;
    @Mock
    private IBitmapCache mBitmapCache;

    private TextLayoutCache textLayoutCache;

    @Before
    public void setUp() {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder()
                .fontResolver(fontResolver)
                .bitmapPool(mBitmapPool)
                .bitmapCache(mBitmapCache)
                .build();
        TypefaceResolver.getInstance().initialize(context, runtimeConfig);
        textLayoutCache = new TextLayoutCache();
        when(mTextProxy.getVisualHash()).thenReturn("HASH");
        when(mTextProxy.getFontWeight()).thenReturn(0);
        when(mTextProxy.getColor()).thenReturn(Color.WHITE);
        when(mTextProxy.getFontFamily()).thenReturn(DEFAULT_FONT_FAMILY);
        when(mTextProxy.getFontLanguage()).thenReturn(TEXT_FONT_LANGUAGE);
        when(mTextProxy.getFontSize()).thenReturn(Dimension.create(24f));
        when(mTextProxy.getFontWeight()).thenReturn(1);
        when(mTextProxy.getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
    }

    @Test
    public void testCacheHit() {
        textLayoutCache.putLayout("hashKey1", mLayout);
        Layout result = textLayoutCache.getLayout("hashKey1");
        assertEquals(mLayout, result);
    }

    @Test
    public void testCacheMiss() {
        textLayoutCache.putLayout("hashKey1", mLayout);
        Layout result = textLayoutCache.getLayout("nope");
        assertNull(result);
    }

    @Test
    public void testCacheClear() {
        textLayoutCache.putLayout("hashKey1", mLayout);
        textLayoutCache.clear();
        Layout result = textLayoutCache.getLayout("hashKey1");
        assertNull(result);
    }


    @Test
    public void testTextPaintCacheReturnsObjectsBasedOnProvidedKey() {
        Paint paint = textLayoutCache.getOrCreateTextPaint(1, "key", mTextProxy, 1.0f);

        // Cache returns previously created Paint
        assertEquals(paint, textLayoutCache.getOrCreateTextPaint(1, "key", mTextProxy, 1.0f));

        // Cache returns a new paint
        assertNotEquals(paint, textLayoutCache.getOrCreateTextPaint(1, "key2", mTextProxy, 1.0f));

        // Cache does not directly depend on visual hash
        verify(mTextProxy, never()).getVisualHash();
    }

    @Test
    public void testHintingDisabledForEmberFontFamilies() {
        for (String fontFamily : FontConstant.fontFamiliesAttachedToAmazonEmber) {
            when(mTextProxy.getFontFamily()).thenReturn(fontFamily);

            Paint paint = textLayoutCache.getOrCreateTextPaint(1, "key", mTextProxy, 1.0f);
            assertEquals(Paint.HINTING_OFF, paint.getHinting());
        }
    }
}
