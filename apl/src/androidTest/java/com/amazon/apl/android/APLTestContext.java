/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.view.TextureView;
import android.view.View;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.FluidityIncidentReporter;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Assert;

import java.util.HashMap;

import androidx.test.platform.app.InstrumentationRegistry;

import static junit.framework.TestCase.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This abstract test class provides a basic framework for testing an APL
 * Component.  It supports validation of properties, jni integration, and
 * view creation.  This base class will test  features common to all components and
 * call into base classes for Component specific features.
 */
@SuppressWarnings("WeakerAccess")
public class APLTestContext {


    // The formatted document
    private String mDocument;
    private HashMap<String, String> mPayload = new HashMap<>();
    private HashMap<String, String> mImport = new HashMap<>();

    // Content
    private Content mContent;

    // Display metrics.
    private ViewportMetrics mMetrics;

    // Configurable options
    private APLOptions mAplOptions;

    // Root Config
    private RootConfig mRootConfig;

    // RootContext - detached from APLLayout
    private RootContext mRootContext;

    // Android Context
    private Context mContext;

    // The view context bv
    private IAPLViewPresenter mPresenter = mock(IAPLViewPresenter.class);

    private IMediaPlayer mMockMediaPlayer = mock(IMediaPlayer.class);

    private IMetricsRecorder mMetricsRecorder = mock(IMetricsRecorder.class);

    private FluidityIncidentReporter mFluidityIncidentReporter = mock(FluidityIncidentReporter.class);

    public Content buildContent() {
        if (mContent == null) {
            try {
                mContent = Content.create(mDocument, new Content.Callback() {
                    @Override
                    public void onDataRequest(Content content, String dataId) {
                        content.addData(dataId, mPayload.get(dataId));
                    }

                    @Override
                    public void onPackageRequest(Content content, Content.ImportRequest request) {
                        try {
                            content.addPackage(request, mImport.get(request.getPackageName()));
                        } catch (Content.ContentException e) {
                            fail("Invalid Import Request - " + e.getMessage());
                        }
                    }
                });
            } catch (Content.ContentException e) {
                Assert.fail(e.getMessage());
            }
        }

        return mContent;
    }

    public APLOptions buildOptions() {
        if (mAplOptions == null)
            mAplOptions = APLOptions.builder().build();
        return mAplOptions;
    }

    public ViewportMetrics buildMetrics() {

        if (mAplOptions == null) {
            buildOptions();
        }

        if (mMetrics == null)
            mMetrics = ViewportMetrics.builder()
                    .width(2048)
                    .height(1024)
                    .dpi(160)
                    .shape(ScreenShape.RECTANGLE)
                    .theme("dark")
                    .mode(ViewportMode.kViewportModeHub)
                    .build();
        return mMetrics;
    }


    public RootConfig buildRootConfig() {
        if (mRootConfig == null) {
            mRootConfig = RootConfig.create("Unit Test", "1.0")
                    .registerDataSource("dynamicIndexList")
                    // On heavy load, the speed of the Espresso `swipeLeft` and `swipeRight` actions
                    // can fall below the threshold and not register as swipes to core.
                    // We need to lower the threshold so that the swipes get detected properly.
                    .set(RootProperty.kTapOrScrollTimeout, 50) // Half of the Fast swipe speed.
                    .pagerChildCache(3)
                    .sequenceChildCache(3)
                    .mediaPlayerFactory(new RuntimeMediaPlayerFactory(new MediaPlayerProvider()));
        }
        return mRootConfig;
    }

    private class MediaPlayerProvider extends AbstractMediaPlayerProvider<View> {

        @Override
        public IMediaPlayer createPlayer(Context context, View view) {
            return mMockMediaPlayer;
        }

        @Override
        public View createView(Context context) {
            return new TextureView(context);
        }
    }

    public IAPLViewPresenter buildPresenter() {
        when(mPresenter.getContext()).thenReturn(InstrumentationRegistry.getInstrumentation().getContext());
        when(mPresenter.getOrCreateViewportMetrics()).thenReturn(mMetrics);
        when(mPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        return mPresenter;
    }

    public IMetricsRecorder buildMetricsRecorder() {
        when(mMetricsRecorder.createCounter(any(String.class))).thenReturn(mock(ICounter.class));
        return mMetricsRecorder;
    }


    public APLTestContext buildRootContextDependencies() {
        buildContent();
        buildOptions();
        buildMetrics();
        buildRootConfig();
        buildPresenter();
        buildMetricsRecorder();
        return this;
    }


    public RootContext buildRootContext() {
        APLController.setRuntimeConfig(RuntimeConfig.builder().build());
        buildRootContextDependencies();

        if (mRootContext == null)
            mRootContext = RootContext.create(mMetrics, mContent, mRootConfig, mAplOptions, mPresenter, mMetricsRecorder, mFluidityIncidentReporter);

        return mRootContext;
    }


    /**
     * Gets a component from the RootContext created by the test doc.  This method
     * first looks for a component named 'testcomp', if not found it returns the top component.
     *
     * @return the component named 'testcomp', otherwise the "Top" component in the document.
     */
    @SuppressWarnings("unchecked")
    public <C extends Component> C getTestComponent() {
        Component component = mRootContext.findComponentById("testcomp");
        if (component != null) {
            return (C) component;
        }
        return (C) mRootContext.getTopComponent();
    }

    /**
     * Gets the View from the RootContext created by the test component.
     *
     * @return the View associated with the test Component.
     */
    @SuppressWarnings("unchecked")
    public <V extends View> V getTestView() {
        return (V) mPresenter.findView(getTestComponent());
    }


    public String getDocument() {
        return mDocument;
    }

    public APLTestContext setDocument(String document) {
        this.mDocument = document;
        return this;
    }

    public APLTestContext setDocument(String document, Object... args) {
        this.mDocument = String.format(document, args);
        return this;
    }

    public APLTestContext setDocumentPayload(String payloadId, String payload) {
        this.mPayload.put(payloadId, payload);
        return this;
    }

    public APLTestContext setDocumentImport(String importId, String importContent) {
        this.mImport.put(importId, importContent);
        return this;
    }

    public Content getContent() {
        return mContent;
    }

    public APLTestContext setContent(Content content) {
        this.mContent = content;
        return this;
    }

    public ViewportMetrics getMetrics() {
        return mMetrics;
    }

    public APLTestContext setMetrics(ViewportMetrics mMetrics) {
        this.mMetrics = mMetrics;
        return this;
    }

    public RootContext getRootContext() {
        return mRootContext;
    }

    public APLTestContext setRootContext(RootContext rootContext) {
        this.mRootContext = rootContext;
        return this;
    }

    public APLTestContext setRootConfig(RootConfig rootConfig) {
        this.mRootConfig = rootConfig;
        return this;
    }

    public RootConfig getRootConfig() {
        return mRootConfig;
    }

    public APLOptions getAplOptions() {
        return mAplOptions;
    }

    public APLTestContext setAplOptions(APLOptions mAplOptions) {
        this.mAplOptions = mAplOptions;
        return this;
    }

    public Context getContext() {
        return mContext;
    }

    public APLTestContext setContext(Context mContext) {
        this.mContext = mContext;
        return this;
    }

    public IAPLViewPresenter getPresenter() {
        return mPresenter;
    }

    public APLTestContext setPresenter(IAPLViewPresenter presenter) {
        this.mPresenter = presenter;
        return this;
    }


}
