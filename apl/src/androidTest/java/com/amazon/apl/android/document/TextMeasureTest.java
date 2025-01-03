/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLViewhostTest;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.RuntimeConfig;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.TextLayoutFactory;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.TextProxy;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.impl.MetricsRecorder;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.FluidityIncidentReporter;
import com.amazon.apl.enums.Display;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;
import com.amazon.apl.enums.ViewportMode;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import static com.amazon.apl.android.TextMeasure.MeasureMode.AtMost;
import static com.amazon.apl.android.TextMeasure.MeasureMode.Exactly;
import static com.amazon.apl.android.TextMeasure.MeasureMode.Undefined;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

// These tests heavily rely on the behaviour of Android TextPaint and TypeFace which isn't replicated 1-1 with Robolectric.
@RunWith(AndroidJUnit4.class)
public class TextMeasureTest extends APLViewhostTest {
    static {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder().fontResolver(new CompatFontResolver()).build();
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext(), runtimeConfig);
    }

    @Mock
    StyledText mockStyledText;
    @Mock
    TextProxy mockTextProxy;

    ViewportMetrics mMetrics = ViewportMetrics.builder()
            .width(2048)
            .height(1024)
            .dpi(160)
            .shape(ScreenShape.RECTANGLE)
            .theme("dark")
            .mode(ViewportMode.kViewportModeHub)
            .build();
    RenderingContext mRenderingContext = RenderingContext.builder()
            .metricsTransform(MetricsTransform.create(mMetrics))
            .aplTrace(new APLTrace("agent"))
            .build();
    TextMeasure mMeasureSpy;
    static String TEXT_HASH = "texthash";
    static String EDIT_HASH = "edithash";


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(mockStyledText.getText(any(), any(), any())).thenReturn("");

        // Text Proxy Modk
        when(mockTextProxy.getVisualHash()).thenReturn(TEXT_HASH);
        when(mockTextProxy.getTextAlignment()).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getDirectionHeuristic()).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getColor()).thenReturn(0xFFFAFAFA);
        when(mockTextProxy.getDisplay()).thenReturn(Display.kDisplayNormal);
        when(mockTextProxy.getFontSize()).thenReturn(40.0f);
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
        when(mockTextProxy.getSize()).thenReturn(8);
        when(mockTextProxy.getTextPaint(anyFloat())).then(InvocationOnMock::callRealMethod);
        when(mockTextProxy.getStyledText()).thenReturn(mockStyledText);

        TextMeasure measure = new TextMeasure(mRenderingContext);
        mMeasureSpy = spy(measure);
    }

    @After
    public void teardown() {
        // Need to clear singleton cache because fake TEXT_HASH causes collisions
        TextLayoutFactory.defaultFactory().clear();
    }

    private RootContext inflateDoc(String doc) {
        APLOptions mOptions = APLOptions.builder().build();
        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0");
        IAPLViewPresenter mPresenter = mock(IAPLViewPresenter.class);
        MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
        FluidityIncidentReporter fluidityIncidentCounter = mock(FluidityIncidentReporter.class);
        ICounter counter = mock(ICounter.class);
        ITimer timer = mock(ITimer.class);
        when(mPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));

        Content content = null;
        try {
            content = Content.create(doc, mOptions);
        } catch (Content.ContentException e) {
            fail("Content failed to inflate");
        }
        if (content == null || !content.isReady()) {
            fail("Content failed to inflate");
        }
        when(metricsRecorder.createCounter(anyString())).thenReturn(counter);
        when(metricsRecorder.startTimer(anyString(), any())).thenReturn(timer);
        RootContext ctx = RootContext.create(mMetrics, content, rootConfig,
                mOptions, mPresenter, metricsRecorder, fluidityIncidentCounter);
        if (ctx == null || ctx.getNativeHandle() == 0) {
            fail("The document failed to load.");
        }
        return ctx;
    }

    @Test
    @SmallTest
    @Ignore("Does not make sense, call should not even happen from core side.")
    public void testMeasureContent_DisplayNone() {
        // when not displayed, return the proposed measurements
        when(mockTextProxy.getDisplay()).thenReturn(Display.kDisplayNone);
        APLTextLayout layout = mMeasureSpy.measure(mockTextProxy, 160, Undefined, 150, Undefined, mockStyledText);
        float[] measure = layout.getSize();
        assertEquals(160, measure[0], 0);
        assertEquals(150, measure[1], 0);
    }

    @Test
    @SmallTest
    public void testMeasure_ModeUndefined() {
        // undefined returns a single line height, and no width
        APLTextLayout layout = mMeasureSpy.measure(mockTextProxy, 64, Undefined, 200, Undefined, mockStyledText);
        float[] measure = layout.getSize();
        assertEquals(0, measure[0], 0);
        assertEquals(51, measure[1], 0);
    }

    @Test
    @SmallTest
    public void testMeasure_ModeExactly() {
        // MeasureMode.Exactly happens when the text has a width/height explicitly set,
        // in this case measureTextContent isn't called until the view is created
        APLTextLayout layout = mMeasureSpy.measure(mockTextProxy, 1011, Exactly, 641, Exactly, mockStyledText);
        float[] measure = layout.getSize();
        assertEquals(1011, measure[0], 0);
        assertEquals(641, measure[1], 0);
    }


    @Test
    @SmallTest
    public void testMeasure_AtMostBoring() {
        APLTextLayout layout = mMeasureSpy.measure(mockTextProxy, 1011, AtMost, 641, AtMost, mockStyledText);
        float[] measure = layout.getSize();
        assertEquals(0, measure[0], 0);
        assertEquals(51, measure[1], 0);
    }

    @Test
    @SmallTest
    public void testMeasure_ModeAtMostExciting() {
        when(mockStyledText.getText(any(), any(), any())).thenReturn("77°F");
        APLTextLayout layout = mMeasureSpy.measure(mockTextProxy, 1011, AtMost, 641, AtMost, mockStyledText);
        float[] measure = layout.getSize();
        assertEquals(100, measure[0], 25); // rough width of a string shorter than alloted width
        assertEquals(51, measure[1], 0);
    }

    @Test
    @SmallTest
    public void testMeasure_ModeAtMostTruncate() {
        when(mockStyledText.getText(any(), any(), any())).thenReturn("And here's to you, Mrs. Robinson Jesus loves you more than you will know");
        APLTextLayout layout = mMeasureSpy.measure(mockTextProxy, 160, AtMost, 150, AtMost, mockStyledText);
        float[] measure = layout.getSize();
        assertEquals(160, measure[0], 0);
        assertEquals(150, measure[1], 0);
    }


    @Test
    @SmallTest
    public void testMeasure_WithSpanTag() {
        String doc =
                "{\n" +
                        "  \"type\": \"APL\",\n" +
                        "  \"version\": \"1.7\",\n" +
                        "  \"mainTemplate\": {\n" +
                        "    \"item\": {\n" +
                        "      \"type\": \"Text\",\n" +
                        "      \"text\": \"And here's to you, <span fontSize='100px'>Mrs. Robinson</span> Jesus loves you more than you will know\",\n" +
                        "      \"fontSize\": \"20px\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
        RootContext ctx = inflateDoc(doc); // span is a pain to mock, so grab a real component
        Text text = (Text)ctx.getTopComponent();
        TextMeasure textMeasure = new TextMeasure(ctx.getRenderingContext());
        APLTextLayout layout = textMeasure.measure(text.getProxy(), mMetrics.width(), AtMost, mMetrics.height(), AtMost, text.getProxy().getStyledText());
        float[] measure = layout.getSize();
        assertTrue(measure[1] > 100 && measure[1] < 150);
    }


    @Test
    @SmallTest
    public void testMeasure_WithTwoSpanTag() {
        String doc =
                "{\n" +
                        "  \"type\": \"APL\",\n" +
                        "  \"version\": \"1.7\",\n" +
                        "  \"mainTemplate\": {\n" +
                        "    \"item\": {\n" +
                        "      \"type\": \"Text\",\n" +
                        "      \"text\": \"And here's to you, <span fontSize='100px'>Mrs. Robinson</span><br> <span fontSize='50px'>Jesus</span> loves you more than you will know\",\n" +
                        "      \"fontSize\": \"20px\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
        RootContext ctx = inflateDoc(doc); // span is a pain to mock, so grab a real component
        Text text = (Text) ctx.getTopComponent();
        TextMeasure textMeasure = new TextMeasure(ctx.getRenderingContext());
        APLTextLayout layout = textMeasure.measure(text.getProxy(), mMetrics.width(), AtMost, mMetrics.height(), AtMost, text.getProxy().getStyledText());
        float[] measure = layout.getSize();
        assertTrue(measure[1] > 150 && measure[1] < 200);
    }


    /**
     * Verify that component width is calculated based on size.
     */
    @Test
    @SmallTest
    public void testMeasure_EditSize() {
        when(mockTextProxy.getSize()).thenReturn(8);
        float[] measure = mMeasureSpy.measureEditText(mockTextProxy, mMetrics.width(), AtMost, mMetrics.height(), AtMost, 8);
        assertEquals(51, measure[1], 0); //single height

        int halfSize = 8 / 2;
        float halfWidth = measure[0] / 2f;

        when(mockTextProxy.getSize()).thenReturn(halfSize);
        measure = mMeasureSpy.measureEditText(mockTextProxy, mMetrics.width(), AtMost, 960, AtMost, halfSize);
        assertEquals(halfWidth, measure[0], 0);
        assertEquals(51, measure[1], 0); //single height
    }

    /**
     * Verify that component height is calculated based on fontSize.
     */
    @Test
    @SmallTest
    public void testMeasure_EditFontSize() {
        when(mockTextProxy.getSize()).thenReturn(1); // Use a single character to eliminate letter spacing

        float[] measure = mMeasureSpy.measureEditText(mockTextProxy, mMetrics.width(), AtMost, mMetrics.height(), AtMost, 1);
        assertEquals(51, measure[1], 0); //single height

        float doubleFont = mockTextProxy.getFontSize() * 2;
        float doubleWidth = measure[0] * 2f;
        float doubleHeight = measure[1] * 2f;

        when(mockTextProxy.getFontSize()).thenReturn(doubleFont);
        measure = mMeasureSpy.measureEditText(mockTextProxy, mMetrics.width(), AtMost, mMetrics.height(), AtMost, 1);
        assertEquals(doubleWidth, measure[0], 2); //double width, allow for rounding
        assertEquals(doubleHeight, measure[1], 2); //double height, allow for rounding
    }
}
