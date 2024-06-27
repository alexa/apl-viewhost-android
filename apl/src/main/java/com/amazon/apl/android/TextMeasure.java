/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.text.Layout;

import androidx.annotation.NonNull;

import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.TracePoint;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;

/**
 * Android text measurement utility.
 */
public class TextMeasure {
    private static final String TAG = "TextMeasure";

    public enum MeasureMode {
        Undefined,
        Exactly,
        AtMost
    }

    static final MeasureMode[] MEASURE_MODES = MeasureMode.values();

    // Text measure metrics
    private static final String METRIC_MEASURE = TAG + ".measure";
    private final int cMeasureTextTotalTime;
    private static final String METRIC_MEASURE_COUNT = TAG + ".measureCount";
    private final int cMeasureText;

    private final IMetricsTransform mMetricsTransform;
    private final ITelemetryProvider mTelemetry;
    private final int mVersionCode;
    private final TextLayoutFactory mTextLayoutFactory;
    private final APLTrace mAplTrace;
    private boolean mIsInitialRenderPass = true;


    public TextMeasure(@NonNull RenderingContext renderingContext) {

        // APL document version
        mVersionCode = renderingContext.getDocVersion();
        mTextLayoutFactory = renderingContext.getTextLayoutFactory();
        mMetricsTransform = renderingContext.getMetricsTransform();
        mAplTrace = renderingContext.getAplTrace();

        mTelemetry = renderingContext.getTelemetryProvider();
        cMeasureTextTotalTime = mTelemetry.createMetricId(APL_DOMAIN, METRIC_MEASURE, COUNTER);
        cMeasureText = mTelemetry.createMetricId(APL_DOMAIN, METRIC_MEASURE_COUNT, COUNTER);
    }

    public void onRootContextCreated() {
        mIsInitialRenderPass = false;
    }

    /**
     * Measure a text based component for display.
     *
     * @param widthDp       Horizontal pixel width dimension as imposed by the parent.
     * @param widthMode     Horizontal width requirements as imposed by the parent.
     * @param heightDp      Vertical pixel width dimension as imposed by the parent.
     * @param heightMode    Vertical width requirements as imposed by the parent.
     * @return pixel based measurement results.
     */
    public APLTextLayout measure(
                          ITextProxy textProxy,
                          float widthDp, MeasureMode widthMode,
                          float heightDp, MeasureMode heightMode,
                          final StyledText text) {
        mAplTrace.startTrace(TracePoint.TEXT_MEASURE);
        long start = 0;
        // track time and frequency

        // Any text measurements that occur after initial render are meaningless for tracking time to first frame.
        // If at some point we want to track successive measures we should be specific about their impact on performance
        // as a long-running document could accrue many of these without any performance concerns.
        if (mIsInitialRenderPass) {
            start = System.currentTimeMillis();
            mTelemetry.incrementCount(cMeasureText);
        }

        // Layout is always limited to the provided height, regardless of mode, for Undefined it's
        // just really big (INF).
        APLTextLayout layout = measureTextContent(textProxy, widthDp, widthMode,
                heightDp, heightMode, text, mMetricsTransform);

        // Allow layout to return DP.
        layout.applyMetricsTransform(mMetricsTransform);

        if (mIsInitialRenderPass) {
            long duration = System.currentTimeMillis() - start;
            mTelemetry.incrementCount(cMeasureTextTotalTime, (int) duration);
        }

        mAplTrace.endTrace();
        return layout;
    }

    public float[] measureEditText(ITextProxy textProxy,
                                  float widthDp, MeasureMode widthMode,
                                  float heightDp, MeasureMode heightMode,
                                  final int size) {
        mAplTrace.startTrace(TracePoint.TEXT_MEASURE);
        long start = 0;
        // track time and frequency

        // Any text measurements that occur after initial render are meaningless for tracking time to first frame.
        // If at some point we want to track successive measures we should be specific about their impact on performance
        // as a long-running document could accrue many of these without any performance concerns.
        if (mIsInitialRenderPass) {
            start = System.currentTimeMillis();
            mTelemetry.incrementCount(cMeasureText);
        }

        // Layout is calculated in Px.
        final float widthPx = mMetricsTransform.toViewhost(widthDp);
        final float heightPx = mMetricsTransform.toViewhost(heightDp);

        Layout layout = mTextLayoutFactory.createEditTextLayout(mVersionCode,
                textProxy,
                Math.round(widthPx),
                Math.round(heightPx),
                size);
        if (mIsInitialRenderPass) {
            long duration = System.currentTimeMillis() - start;
            mTelemetry.incrementCount(cMeasureTextTotalTime, (int) duration);
        }

        mAplTrace.endTrace();
        return new float[] {
                mMetricsTransform.toCore(layout.getWidth()),
                mMetricsTransform.toCore(ITextProxy.adjustHeightByMode(heightPx, heightMode, layout)),
                mMetricsTransform.toCore(layout.getLineBaseline(0))};
    }

    /**
     * Measure the Text Component. This method builds a StaticLayout based on the Component
     * properties and saves it in {@link TextLayoutCache}.
     *
     * @param textProxy  Proxy for component property lookup.
     * @param widthDp    Horizontal width dimension as imposed by the parent in dp.
     * @param widthMode  Horizontal width requirements as imposed by the parent.
     * @param heightDp   Vertical width dimension as imposed by the parent in dp.
     * @param heightMode  Horizontal width requirements as imposed by the parent.
     * @param text  The {@link StyledText} for this this text.
     * @param metricsTransform  The {@link IMetricsTransform} to use to convert dp values to px
     */
    private APLTextLayout measureTextContent(
            ITextProxy textProxy,
            final float widthDp, @NonNull final MeasureMode widthMode,
            final float heightDp, @NonNull final MeasureMode heightMode,
            final StyledText text,
            IMetricsTransform metricsTransform) {
        // In all cases for the width measureTextContent mode (undefined, at most, exactly) the text layout
        // should match or fit within the width dimension
        return mTextLayoutFactory.getOrCreateTextLayoutForTextMeasure(mVersionCode, textProxy, text,
                widthDp, widthMode, heightDp, heightMode, metricsTransform);
    }
}
