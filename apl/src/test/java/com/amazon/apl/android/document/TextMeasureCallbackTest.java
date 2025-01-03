/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentState;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.ITextProxy;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.RuntimeConfig;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.TextMeasure.MeasureMode;
import com.amazon.apl.android.TextMeasureCallback;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.impl.MetricsRecorder;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.scenegraph.text.APLTextProperties;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;
import com.amazon.apl.android.utils.FluidityIncidentReporter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TextMeasureCallbackTest extends ViewhostRobolectricTest {
    static {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder().fontResolver(new CompatFontResolver()).build();
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext(), runtimeConfig);
    }


    @Mock
    IAPLViewPresenter mPresenter;
    @Mock
    MetricsRecorder mMetricsRecorder;
    @Mock
    FluidityIncidentReporter mFluidityIncidentReporter;
    @Mock
    ICounter mCounter;
    @Mock
    ITimer mTimer;
    ViewportMetrics mMetrics = ViewportMetrics.builder()
            .width(640)
            .height(480)
            .dpi(160)
            .shape(ScreenShape.RECTANGLE)
            .theme("black")
            .mode(ViewportMode.kViewportModeHub)
            .build();
    APLOptions mOptions = APLOptions.builder().build();
    RootConfig mRootConfig = RootConfig.create();
    TextMeasureCallback mCallback;
    TextMeasure mMeasureSpy;
    private TextMeasureCallback.Factory mFactorySpy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        // Capture TextMeasureCallback calls.  Since TextMeasureCallback is a bound
        // object, and the instance is helled and called from jni, we can't spy the JNI call.
        // Instead, spy on the TextMeasure delegate used by the callback.

        // 1. inject a spy-able instance of the callback factory to capture the creation of callbacks
        mFactorySpy = spy(TextMeasureCallback.Factory.class);
        TextMeasureCallback.Factory.inject(mFactorySpy);
        Answer<TextMeasureCallback> answer = (invocation -> {
            // 2. when the factory creates a callback, spy on the TextMeasure delegate
            mCallback = (TextMeasureCallback) invocation.callRealMethod();
            mMeasureSpy = spy(mCallback.getDelegate());
            mCallback.delegate(mMeasureSpy);
            return mCallback;
        });
        when(mPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(mPresenter.metricsRecorder()).thenReturn(mMetricsRecorder);
        doAnswer(answer).when(mFactorySpy).create(any(IMetricsTransform.class), any(TextMeasure.class));
        doAnswer(answer).when(mFactorySpy).create(any(RootConfig.class),
                any(IMetricsTransform.class), any(TextMeasure.class));
    }

    private Content mContent;

    private RootContext inflateDoc(String doc) {
        mContent = null;
        try {
            mContent = Content.create(doc, mOptions);
        } catch (Content.ContentException e) {
            fail("Content failed to inflate");
        }
        if (mContent == null || !mContent.isReady()) {
            fail("Content failed to inflate");
        }
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);
        RootContext ctx = RootContext.create(mMetrics, mContent, mRootConfig,
                mOptions, mPresenter, mMetricsRecorder, mFluidityIncidentReporter);
        if (ctx == null || ctx.getNativeHandle() == 0) {
            fail("The document failed to load.");
        }
        return ctx;
    }

    /**
     * Verify the callback is created and bound.
     */
    @Test
    public void test_Bind() {
        String doc =
                "{\n" +
                        "  \"type\": \"APL\",\n" +
                        "  \"version\": \"1.7\",\n" +
                        "  \"mainTemplate\": {\n" +
                        "    \"item\": {\n" +
                        "      \"type\": \"Text\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
        inflateDoc(doc);
        assertTrue(mCallback.isBound());
    }

    String doc =
            "{\n" +
                    "  \"type\": \"APL\",\n" +
                    "  \"version\": \"1.7\",\n" +
                    "  \"mainTemplate\": {\n" +
                    "    \"items\": {\n" +
                    "      \"type\": \"Container\",\n" +
                    "      \"width\": \"100%\",\n" +
                    "      \"height\": \"100%\",\n" +
                    "      \"items\": {\n" +
                    "        \"type\": \"Container\",\n" +
                    "        \"width\": \"auto\",\n" +
                    "        \"height\": \"auto\",\n" +
                    "        \"items\": [\n" +
                    "          {\"type\": \"Text\", \"id\": \"auto1\", \"text\": \"Some text\", \"width\": \"auto\"},\n" +
                    "          {\"type\": \"Text\", \"id\": \"relative1\", \"text\": \"Some text\", \"width\": \"50%\"},\n" +
                    "          {\"type\": \"Text\", \"id\": \"static1\", \"text\": \"Some text\", \"width\": 200, \"height\": 20},\n" +
                    "          {\"type\": \"Text\", \"id\": \"auto2\", \"text\": \"Some text\", \"width\": \"auto\"},\n" +
                    "          {\"type\": \"Text\", \"id\": \"relative2\", \"text\": \"Some text\", \"width\": \"50%\"},\n" +
                    "          {\"type\": \"Text\", \"id\": \"static2\", \"text\": \"Some text\", \"width\": 200, \"height\": 20}\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

    /**
     * The Callback from core happens on inflate
     */
    @Test
    public void test_CallbackInflate() {
        RootContext ctx = inflateDoc(doc);

        // The factory created a callback
        verify(mFactorySpy).create(eq(ctx.getRenderingContext().getMetricsTransform()), any(TextMeasure.class));

        // the first two children should be measured, all other children have the same hash
        // two layout passes are needed, one at the full width and one at 50%
        verify(mMeasureSpy).measure(any(APLTextProperties.class), eq(640.0f), eq(MeasureMode.Exactly), eq(480.0f), eq(MeasureMode.AtMost), any(StyledText.class));
        verify(mMeasureSpy).measure(any(APLTextProperties.class), eq(320.0f), eq(MeasureMode.Exactly), eq(480.0f), eq(MeasureMode.AtMost), any(StyledText.class));
        verify(mMeasureSpy).onRootContextCreated();
        verifyNoMoreInteractions(mMeasureSpy);
    }

    /**
     * When restoring a RootContext a callback is created and bound to a previous callback instance.
     */
    @Test
    public void test_CallbackReInflate() {
        RootContext ctx = inflateDoc(doc);
        // The factory created and used a callback (as tested above)
        verify(mFactorySpy).create(eq(ctx.getRenderingContext().getMetricsTransform()), any(TextMeasure.class));
        verify(mMeasureSpy, times(2)).measure(any(ITextProxy.class), anyFloat(), any(MeasureMode.class), anyFloat(), any(MeasureMode.class), any(StyledText.class));
        verify(mMeasureSpy).onRootContextCreated();
        verifyNoMoreInteractions(mMeasureSpy);

        // get the hash of the existing callback
        long address = mCallback.getNativeAddress();

        // Recreate a root context from a previous context
        DocumentState documentState = new DocumentState(ctx, mContent, 0);
        ctx = RootContext.createFromCachedDocumentState(documentState, mPresenter, mFluidityIncidentReporter);

        verify(mPresenter, times(2)).preDocumentRender();

        // The factory created a new callback from the previous native object
        verify(mFactorySpy).create(eq(mRootConfig), eq(ctx.getRenderingContext().getMetricsTransform()), any(TextMeasure.class));
        assertEquals(address, mCallback.getNativeAddress());

        // The text measure is prepared with the callback as a proxy
        verify(mMeasureSpy).onRootContextCreated();

        // There is no need for callback because the context was already inflated
        verifyNoMoreInteractions(mMeasureSpy);

    }

}
