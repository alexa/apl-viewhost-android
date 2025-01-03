/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.document;

import static com.amazon.apl.android.TextMeasure.MeasureMode.Exactly;
import static com.amazon.apl.android.TextMeasure.MeasureMode.Undefined;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.text.Layout;

import androidx.test.filters.SmallTest;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.TextLayoutFactory;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.TextProxy;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

public class TextMeasureTest extends ViewhostRobolectricTest {

    ViewportMetrics mMetricsMobile = ViewportMetrics.builder()
            .width(2048)
            .height(1080)
            .dpi(440) // 440 is Pixel3a screen DPI
            .shape(ScreenShape.RECTANGLE)
            .theme("dark")
            .mode(ViewportMode.kViewportModeMobile)
            .build();

    TextLayoutFactory tlf = mock(TextLayoutFactory.class);

    RenderingContext mRenderingContextMobile = RenderingContext.builder()
            .metricsTransform(MetricsTransform.create(mMetricsMobile))
            .textLayoutFactory(tlf)
            .aplTrace(new APLTrace("agent"))
            .build();

    static String TEXT_HASH = "texthash";

    @Mock
    TextProxy mockTextProxy;
    @Mock
    StyledText mockStyledText;

    @Before
    public void setup() {
        // Text Proxy Mock
        when(mockStyledText.getText(any(), any(), any())).thenReturn("");
        when(mockTextProxy.getVisualHash()).thenReturn(TEXT_HASH);
        when(mockTextProxy.getTextAlignment()).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getDirectionHeuristic()).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getColor()).thenReturn(0xFFFAFAFA);
        when(mockTextProxy.getDisplay()).thenReturn(Display.kDisplayNormal);
        when(mockTextProxy.getFontSize()).thenReturn(40f);
        when(mockTextProxy.getFontFamily()).thenReturn("sans-serif");
        when(mockTextProxy.getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(mockTextProxy.getFontWeight()).thenReturn(400);
        when(mockTextProxy.getFontLanguage()).thenReturn("en-US");
        when(mockTextProxy.getLetterSpacing()).thenReturn(Dimension.create(0f));
        when(mockTextProxy.getLineHeight()).thenReturn(24.0f);
        when(mockTextProxy.getMaxLines()).thenReturn(0);
        when(mockTextProxy.getTextAlign()).thenReturn(TextAlign.kTextAlignAuto);
        when(mockTextProxy.getTextAlign()).thenReturn(TextAlign.kTextAlignAuto);
        when(mockTextProxy.getTextAlignVertical()).thenReturn(TextAlignVertical.kTextAlignVerticalAuto);
        when(mockTextProxy.getScalingFactor()).thenReturn(1.f);
    }

    @Test
    @SmallTest
    public void testMeasure_transformAndMeasure() {
        TextMeasure textMeasure = new TextMeasure(mRenderingContextMobile);

        Layout layout = mock(Layout.class);
        when(layout.getHeight()).thenReturn(41);
        when(layout.getWidth()).thenReturn(41);

        float expectedLayoutWidthDp = (float)Math.ceil(41f * 160f / mMetricsMobile.dpi());
        float expectedLayoutHeightDp = (float)Math.ceil(41f * 160f / mMetricsMobile.dpi());
        APLTextLayout textLayout = new APLTextLayout(layout, "", false, expectedLayoutWidthDp, expectedLayoutHeightDp);

        when(mockTextProxy.getDisplay()).thenReturn(Display.kDisplayNormal);
        when(tlf.getOrCreateTextLayoutForTextMeasure(anyInt(), eq(mockTextProxy), eq(mockStyledText), eq(500.4f), eq(Exactly), eq(600.4f), eq(Undefined), any())).thenReturn(textLayout);

        APLTextLayout ntl = textMeasure.measure(mockTextProxy, (float)500.4, Exactly, (float)600.4, Undefined, mockStyledText);

        // Measure in Core DPs = (Measure in Screen Fixes * Core DPI)/ (Screen DPI)
        // 160.0f is default Core DPI defined in CoreEngine/content/metric.h; CORE_DPI.
        float expected = (float)Math.ceil(((layout.getWidth()*160.0)/mMetricsMobile.dpi()));

        assertEquals(textLayout, ntl);
        assertEquals(ntl.getSize()[0], expected, 0.0);
        assertEquals(ntl.getSize()[1], expected, 0.0);
    }


}
