package com.amazon.apl.android.document;

import static com.amazon.apl.android.TextMeasure.MeasureMode.Exactly;
import static com.amazon.apl.android.TextMeasure.MeasureMode.Undefined;
import static com.amazon.apl.enums.ComponentType.kComponentTypeText;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.text.Layout;

import androidx.test.filters.SmallTest;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.TextLayoutFactory;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.TextProxy;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.android.scaling.ViewportMetrics;
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

    @Before
    public void setup() {
        // Text Proxy Modk
        when(mockTextProxy.getVisualHash()).thenReturn(TEXT_HASH);
        when(mockTextProxy.getTextAlignment()).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getDirectionHeuristic()).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getColor()).thenReturn(0xFFFAFAFA);
        when(mockTextProxy.getDisplay()).thenReturn(Display.kDisplayNormal);
        when(mockTextProxy.getFontSize()).thenReturn(Dimension.create(40));
        when(mockTextProxy.getFontFamily()).thenReturn("sans-serif");
        when(mockTextProxy.getFontStyle()).thenReturn(FontStyle.kFontStyleNormal);
        when(mockTextProxy.getFontWeight()).thenReturn(400);
        when(mockTextProxy.getFontLanguage()).thenReturn("en-US");
        when(mockTextProxy.getLetterSpacing()).thenReturn(Dimension.create(0f));
        when(mockTextProxy.getLineHeight()).thenReturn(24.0f);
        when(mockTextProxy.getMaxLines()).thenReturn(0);
        when(mockTextProxy.getText(any(), any())).thenReturn("");
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

        // Since in unit-test we use inline-mockmaker, we can
        // mock final methods too.
        when(layout.getWidth()).thenReturn(41);

        when(mockTextProxy.getDisplay()).thenReturn(Display.kDisplayNormal);

        when(tlf.getOrCreateTextLayout(anyInt(), any(), anyInt(), any(), anyInt(),any())).thenReturn(layout);
        textMeasure.prepare(mockTextProxy, null);

        float  mea[] = textMeasure.measure(TEXT_HASH, kComponentTypeText, (float)500.4,Exactly, (float)600.4, Undefined);

        // Measure in Core DPs = (Measure in Screen Fixes * Core DPI)/ (Screen DPI)
        // 160.0f is default Core DPI defined in CoreEngine/content/metric.h; CORE_DPI.
        float expected = (float)((layout.getWidth()*160.0)/mMetricsMobile.dpi());

        assertEquals(expected,mea[0],0.0);
        assertEquals(expected,mea[1],0.0);

    }


}
