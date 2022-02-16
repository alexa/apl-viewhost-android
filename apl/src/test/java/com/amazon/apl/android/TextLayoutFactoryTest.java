/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.graphics.Color;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.font.IFontResolver;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TextLayoutFactoryTest extends ViewhostRobolectricTest {
    private TextLayoutFactory mFactory;
    private LineSpan mKaraokeLine;
    private int mVersionCode;
    private int mInnerWidth;
    private int mInnerHeight;
    private TextMeasure.MeasureMode mWidthMode;
    private Layout mDefaultLayout;

    @Mock
    private Context context;
    @Mock
    private IFontResolver mFontResolver;
    @Mock
    private TextProxy mMockTextProxy;
    @Mock
    private IBitmapPool mBitmapPool;
    @Mock
    private IBitmapCache mBitmapCache;

    @Before
    public void setUp() {
        mFactory = TextLayoutFactory.defaultFactory();
        mKaraokeLine = new LineSpan(0, 0, 0);
        mVersionCode = 1;
        mInnerWidth = 640;
        mInnerHeight = 480;
        mWidthMode = TextMeasure.MeasureMode.Exactly;

        RuntimeConfig runtimeConfig = RuntimeConfig.builder()
                .fontResolver(mFontResolver)
                .bitmapPool(mBitmapPool)
                .bitmapCache(mBitmapCache)
                .build();
        TypefaceResolver.getInstance().initialize(context, runtimeConfig);

        when(mMockTextProxy.getText(any(), any())).thenReturn("My Text");
        when(mMockTextProxy.getColor()).thenReturn(Color.BLACK);
        when(mMockTextProxy.getDirectionHeuristic()).thenReturn(TextDirectionHeuristics.LTR);
        when(mMockTextProxy.getFontFamily()).thenReturn("serif");
        when(mMockTextProxy.getFontLanguage()).thenReturn("en-CA");
        when(mMockTextProxy.getFontSize()).thenReturn(Dimension.create(24f));
        when(mMockTextProxy.getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(mMockTextProxy.getFontWeight()).thenReturn(0);
        when(mMockTextProxy.getFontWeight()).thenReturn(1);
        when(mMockTextProxy.getLineHeight()).thenReturn(1.1f);
        when(mMockTextProxy.getTextAlignment()).thenReturn(Layout.Alignment.ALIGN_NORMAL);
        when(mMockTextProxy.getMaxLines()).thenReturn(1);
        when(mMockTextProxy.getStyledText()).thenReturn(null);
        when(mMockTextProxy.getVisualHash()).thenReturn("visual_hash");
        when(mMockTextProxy.limitLines()).thenReturn(true);
        when(mMockTextProxy.getScalingFactor()).thenReturn(1.f);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
            mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);
    }

    @After
    public void teardown() {
        TextLayoutFactory.defaultFactory().clear();
    }

    @Test
    public void testLayoutReuse() {
        Layout sameLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        assertEquals(mDefaultLayout, sameLayout);
    }

    @Test
    public void testLayoutChangesWithVersionCodeChange() {
        Layout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode + 1, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithWidthModeChange() {
        Layout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, TextMeasure.MeasureMode.AtMost, mInnerHeight, mKaraokeLine);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithKarokeLineSpanChange() {
        Layout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight + 1,
                new LineSpan(0, 0, 1));

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithNullKaraoke() {
        Layout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight,null);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithVisualHashChange() {
        when(mMockTextProxy.getVisualHash()).thenReturn("another_hash");
        Layout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithScalingFactorChange() {
        when(mMockTextProxy.getScalingFactor()).thenReturn(1.2f);
        Layout differentLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        assertNotEquals(mDefaultLayout, differentLayout);
    }

    @Test
    public void testIgnoresChangesNotReflectedInVisualHash() {
        // If the font family really does change, we're counting on core to update the visual hash
        when(mMockTextProxy.getFontFamily()).thenReturn("sans serif");
        Layout sameLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        assertEquals(mDefaultLayout, sameLayout);
    }

    @Test
    public void testLayoutInnerWidthIsBoringLayoutWidth_reusesLayout() {
        // Set the boring metrics width to return 75 px
        TextLayoutFactory.AndroidTextMeasure mockMeasure = mock(TextLayoutFactory.AndroidTextMeasure.class);
        when(mockMeasure.getDesiredTextWidth(anyString(), any(TextPaint.class))).thenReturn(75);
        mFactory = new TextLayoutFactory(mockMeasure);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        // Changing innerwidth to be the size of the desired text width will reuse the layout
        Layout sameLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, 75, mWidthMode, mInnerHeight, mKaraokeLine);

        assertEquals(mDefaultLayout, sameLayout);
    }

    @Test
    public void testLayoutInnerWidthIsLessThanBoringLayoutWidth_createsNewLayout() {
        TextLayoutFactory.AndroidTextMeasure mockMeasure = mock(TextLayoutFactory.AndroidTextMeasure.class);
        when(mockMeasure.getDesiredTextWidth(anyString(), any(TextPaint.class))).thenReturn(75);
        mFactory = new TextLayoutFactory(mockMeasure);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        // Change the bounds to be smaller than the desired text width and we get a new layout
        Layout smallerLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, 74, mWidthMode, mInnerHeight, mKaraokeLine);

        assertNotEquals(mDefaultLayout, smallerLayout);
    }

    @Test
    public void testLayout_RTL_anyWidthChanges_createsNewLayout() {
        when(mMockTextProxy.getDirectionHeuristic()).thenReturn(TextDirectionHeuristics.RTL);
        TextLayoutFactory.AndroidTextMeasure mockMeasure = mock(TextLayoutFactory.AndroidTextMeasure.class);
        when(mockMeasure.getDesiredTextWidth(anyString(), any(TextPaint.class))).thenReturn(75);
        mFactory = new TextLayoutFactory(mockMeasure);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth, mWidthMode, mInnerHeight, mKaraokeLine);

        // Change the bounds to be one pixel smaller
        Layout smallerLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth - 1, mWidthMode, mInnerHeight, mKaraokeLine);
        Layout largerLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth + 1, mWidthMode, mInnerHeight, mKaraokeLine);

        assertNotEquals(mDefaultLayout, smallerLayout);
        assertNotEquals(smallerLayout, largerLayout);
        assertNotEquals(mDefaultLayout, largerLayout);
    }
}
