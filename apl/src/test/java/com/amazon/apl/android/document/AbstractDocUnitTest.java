/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.UserPerceivedFatalReporter;
import com.amazon.apl.android.bitmap.ShadowCache;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.impl.MetricsRecorder;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// Copied from androidTest to ensure maximum compatibility for migrating tests.
public abstract class AbstractDocUnitTest extends ViewhostRobolectricTest {

    /**
     * Utility class for accessing variables inside lambdas
     *
     * @param <T>
     */
    public static class LambdaWrapper<T> {
        private T mWrapped;

        public void set(T value) {
            mWrapped = value;
        }

        public T get() {
            return mWrapped;
        }
    }

    // Load the APL library.
    static {
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext());
    }

    protected long mTime = 0L;
    protected RootContext mRootContext = null;
    protected Content mContent = null;
    protected APLOptions mOptions = null;
    protected RootConfig mRootConfig = null;
    protected ShadowCache mShadowCache;

    protected Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    @Mock
    protected ShadowBitmapRenderer mockShadowRenderer;

    @Mock
    protected IAPLViewPresenter mAPLPresenter;
    @Mock
    protected MetricsRecorder mMetricsRecorder;
    @Mock
    protected ICounter mCounter;
    @Mock
    protected ITimer mTimer;

    @Mock
    protected IVisualContextListener mMockVisualContextListener;

    @Before
    public void initChoreographer() {
        MockitoAnnotations.initMocks(this);
    }

    protected void loadDocument(String doc) {
        loadDocument(doc, buildAplOptions());
    }

    protected void loadDocument(String doc, String payload) {

    }

    protected void loadDocument(String doc, ViewportMetrics metrics) {
        loadDocument(doc, buildAplOptions(), metrics);
    }

    protected void loadDocument(String doc, APLOptions options) {
        ViewportMetrics metrics = ViewportMetrics.builder()
                        .width(1280)
                        .height(720)
                        .dpi(160)
                        .shape(ScreenShape.RECTANGLE)
                        .theme("dark")
                        .mode(ViewportMode.kViewportModeHub)
                        .build();
        loadDocument(doc, options, metrics);
    }

    protected void loadDocument(String doc, APLOptions options, ViewportMetrics metrics) {
        loadDocument(doc, options, metrics, new UserPerceivedFatalReporter());
    }

    protected void loadDocument(String doc, APLOptions options, ViewportMetrics metrics,
                                UserPerceivedFatalReporter userPerceivedFatalReporter) {
        mContent = null;
        try {
            mContent = Content.create(doc);
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }
        assertTrue(mContent.isReady());
        mOptions = options;

        // create a RootContext

        mRootConfig = buildRootConfig();

        mAPLPresenter = mock(IAPLViewPresenter.class);
        mShadowCache = new ShadowCache();
        when(mockShadowRenderer.getCache()).thenReturn(mShadowCache);
        when(mAPLPresenter.getShadowRenderer()).thenReturn(mockShadowRenderer);
        when(mAPLPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(mAPLPresenter.getOrCreateViewportMetrics()).thenReturn(metrics);
        when(mAPLPresenter.metricsRecorder()).thenReturn(mMetricsRecorder);
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);

        mRootContext = spy(RootContext.create(metrics, mContent, mRootConfig, mOptions,
                mAPLPresenter, userPerceivedFatalReporter, mMetricsRecorder));

        assertNotNull(mRootContext);


        mRootContext.initTime();
        mTime = 100;
        mRootContext.onTick(mTime);
    }

    /**
     * Updates the root context by time `delta`
     *
     * @param delta The amount of time to update in ms
     */
    protected void update(long delta) {
        if (mRootContext != null) {
            mTime += delta * 1000000;
            mRootContext.onTick(mTime);
        }
    }

    protected RootConfig buildRootConfig() {
        if (mRootConfig == null)
            mRootConfig = RootConfig.create("Unit Test", "1.0")
                .allowOpenUrl(true);
        return mRootConfig;
    }

    protected APLOptions buildAplOptions() {
        if (mOptions == null)
            mOptions = APLOptions.builder()
                    .aplClockProvider(callback -> new TestClock(callback))
                    .build();
        return mOptions;
    }
}
