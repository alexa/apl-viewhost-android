/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.font.IFontResolver;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.text.LineSpan;
import com.amazon.apl.enums.FontStyle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TextLayoutFactoryTest extends ViewhostRobolectricTest {
    private TextLayoutFactory mFactory;
    private LineSpan mKaraokeLine;
    private int mVersionCode;
    private int mInnerWidth;
    private int mInnerHeight;
    private TextMeasure.MeasureMode mWidthMode;
    private TextMeasure.MeasureMode mHeightMode;
    private APLTextLayout mDefaultLayout;
    private APLTextLayout mDefaultMeasurementLayout;

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
    @Mock
    private StyledText mStyledText;
    @Mock
    private IMetricsTransform mMetricsTransform;

    @Before
    public void setUp() {
        mFactory = TextLayoutFactory.defaultFactory();
        mKaraokeLine = new LineSpan(0, 0, 0);
        mVersionCode = 1;
        mInnerWidth = 640;
        mInnerHeight = 480;
        mWidthMode = TextMeasure.MeasureMode.Exactly;
        mHeightMode = TextMeasure.MeasureMode.Exactly;

        RuntimeConfig runtimeConfig = RuntimeConfig.builder()
                .fontResolver(mFontResolver)
                .bitmapPool(mBitmapPool)
                .bitmapCache(mBitmapCache)
                .build();
        TypefaceResolver.getInstance().initialize(context, runtimeConfig);

        when(mStyledText.getText(any(), any(), any())).thenReturn("My Text");
        when(mStyledText.getHash()).thenReturn("text_hash");
        when(mMockTextProxy.getStyledText()).thenReturn(mStyledText);
        when(mMockTextProxy.getColor()).thenReturn(Color.BLACK);
        when(mMockTextProxy.getDirectionHeuristic()).thenReturn(TextDirectionHeuristics.LTR);
        when(mMockTextProxy.getFontFamily()).thenReturn("serif");
        when(mMockTextProxy.getFontLanguage()).thenReturn("en-CA");
        when(mMockTextProxy.getFontSize()).thenReturn(24f);
        when(mMockTextProxy.getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(mMockTextProxy.getFontWeight()).thenReturn(0);
        when(mMockTextProxy.getFontWeight()).thenReturn(1);
        when(mMockTextProxy.getLineHeight()).thenReturn(1.1f);
        when(mMockTextProxy.getTextAlignment()).thenReturn(Layout.Alignment.ALIGN_NORMAL);
        when(mMockTextProxy.getMaxLines()).thenReturn(1);
        when(mMockTextProxy.getVisualHash()).thenReturn("visual_hash");
        when(mMockTextProxy.limitLines()).thenReturn(true);
        when(mMockTextProxy.getScalingFactor()).thenReturn(1.f);

        when(mMetricsTransform.toViewhost(anyFloat())).thenAnswer(invocation -> {
            Float argument = invocation.getArgument(0);
            return argument;
        });

        when(mMetricsTransform.toCore(anyFloat())).thenAnswer(invocation -> {
            Float argument = invocation.getArgument(0);
            return argument;
        });

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
            mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);
        mDefaultMeasurementLayout = mFactory.getOrCreateTextLayoutForTextMeasure(
                mVersionCode, mMockTextProxy, mStyledText, mInnerWidth,
                mWidthMode, mInnerHeight, mHeightMode, mMetricsTransform);
    }

    @After
    public void teardown() {
        TextLayoutFactory.defaultFactory().clear();
    }

    @Test
    public void testLayoutReuse() {
        APLTextLayout sameLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertEquals(mDefaultLayout, sameLayout);
    }

    @Test
    public void testLayoutChangesWithVersionCodeChange() {
        APLTextLayout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode + 1, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithWidthModeChange() {
        APLTextLayout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, TextMeasure.MeasureMode.AtMost, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithKarokeLineSpanChange() {
        APLTextLayout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight + 1, mHeightMode,
                new LineSpan(0, 0, 1), mMetricsTransform);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithNullKaraoke() {
        APLTextLayout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode,null, mMetricsTransform);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithVisualHashChange() {
        when(mMockTextProxy.getVisualHash()).thenReturn("another_hash");
        APLTextLayout diffLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertNotEquals(mDefaultLayout, diffLayout);
    }

    @Test
    public void testLayoutChangesWithScalingFactorChange() {
        when(mMockTextProxy.getScalingFactor()).thenReturn(1.2f);
        APLTextLayout differentLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertNotEquals(mDefaultLayout, differentLayout);
    }

    @Test
    public void testIgnoresChangesNotReflectedInVisualHash() {
        // If the font family really does change, we're counting on core to update the visual hash
        when(mMockTextProxy.getFontFamily()).thenReturn("sans serif");
        APLTextLayout sameLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertEquals(mDefaultLayout, sameLayout);
    }

    @Test
    public void testLayoutInnerWidthIsBoringLayoutWidth_reusesLayout() {
        // Set the boring metrics width to return 75 px
        TextLayoutFactory.AndroidTextMeasure mockMeasure = mock(TextLayoutFactory.AndroidTextMeasure.class);
        when(mockMeasure.getDesiredTextWidth(anyString(), any(TextPaint.class))).thenReturn(75);
        mFactory = new TextLayoutFactory(mockMeasure);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        // Changing innerwidth to be the size of the desired text width will reuse the layout
        APLTextLayout sameLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, 75, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertEquals(mDefaultLayout, sameLayout);
    }

    @Test
    public void testLayoutInnerWidthIsLessThanBoringLayoutWidth_createsNewLayout() {
        TextLayoutFactory.AndroidTextMeasure mockMeasure = mock(TextLayoutFactory.AndroidTextMeasure.class);
        when(mockMeasure.getDesiredTextWidth(anyString(), any(TextPaint.class))).thenReturn(75);
        mFactory = new TextLayoutFactory(mockMeasure);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        // Change the bounds to be smaller than the desired text width and we get a new layout
        APLTextLayout smallerLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, 74, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertNotEquals(mDefaultLayout, smallerLayout);
    }

    @Test
    public void testLayout_RTL_anyWidthChanges_createsNewLayout() {
        when(mMockTextProxy.getDirectionHeuristic()).thenReturn(TextDirectionHeuristics.RTL);
        TextLayoutFactory.AndroidTextMeasure mockMeasure = mock(TextLayoutFactory.AndroidTextMeasure.class);
        when(mockMeasure.getDesiredTextWidth(anyString(), any(TextPaint.class))).thenReturn(75);
        mFactory = new TextLayoutFactory(mockMeasure);

        mDefaultLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        // Change the bounds to be one pixel smaller
        APLTextLayout smallerLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth - 1, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);
        APLTextLayout largerLayout = mFactory.getOrCreateTextLayout(mVersionCode, mMockTextProxy, mInnerWidth + 1, mWidthMode, mInnerHeight, mHeightMode, mKaraokeLine, mMetricsTransform);

        assertNotEquals(mDefaultLayout, smallerLayout);
        assertNotEquals(smallerLayout, largerLayout);
        assertNotEquals(mDefaultLayout, largerLayout);
    }

    @Test
    public void test_measurementLayout_default_layoutReuse() {
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mMetricsTransform);

        assertEquals(mDefaultMeasurementLayout, newLayout);
    }

    @Test
    public void test_measurementLayout_differentWidth_createsNewLayout() {
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth + 1, mWidthMode, mInnerHeight, mHeightMode, mMetricsTransform);

        assertNotEquals(mDefaultMeasurementLayout, newLayout);
    }

    @Test
    public void test_measurementLayout_differentHeight_createsNewLayout() {
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth, mWidthMode, mInnerHeight + 1, mHeightMode, mMetricsTransform);

        assertNotEquals(mDefaultMeasurementLayout, newLayout);
    }

    @Test
    public void test_measurementLayout_differentWidthMode_createsNewLayout() {
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth, TextMeasure.MeasureMode.AtMost, mInnerHeight, mHeightMode, mMetricsTransform);

        assertNotEquals(mDefaultMeasurementLayout, newLayout);
    }

    @Test
    public void test_measurementLayout_differentHeightMode_createsNewLayout() {
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth, mWidthMode, mInnerHeight, TextMeasure.MeasureMode.AtMost, mMetricsTransform);

        assertNotEquals(mDefaultMeasurementLayout, newLayout);
    }

    @Test
    public void test_measurementLayout_differentTextPropertyHash_createsNewLayout() {
        // Relying on Core to update the hash for APLProperties
        when(mMockTextProxy.getVisualHash()).thenReturn("different_visual_hash");
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mMetricsTransform);

        assertNotEquals(mDefaultMeasurementLayout, newLayout);
    }

    @Test
    public void test_measurementLayout_differentStyledTextHash_createsNewLayout() {
        // Relying on Core to update the hash for TextChunk
        when(mStyledText.getHash()).thenReturn("different_text_hash");
        APLTextLayout newLayout = mFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, mMockTextProxy, mStyledText,
                mInnerWidth, mWidthMode, mInnerHeight, mHeightMode, mMetricsTransform);

        assertNotEquals(mDefaultMeasurementLayout, newLayout);
    }
}
