/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.os.SystemClock;
import android.text.Layout;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.Display;

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

    // Proxy for Text property lookup
    private TextProxy mTextProxy;
    // Proxy for EditText property lookup
    private EditTextProxy mEditTextProxy;
    private final APLTrace mAplTrace;

    // The measured width in pixels
    private int mMeasuredWidthPx = 0;
    // The measured height in pixels
    private int mMeasuredHeightPx = 0;
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
     * Prepare for text measurement.  This method is guaranteed to be called once before
     * measure to assign the proxy for Component property lookup. measure may be called
     * one or more times per prepare call.
     *
     * @param textProxy     Proxy for Text property lookup.
     * @param editTextProxy Proxy for EditText property lookup.
     */
    public void prepare(TextProxy textProxy, EditTextProxy editTextProxy) {
        mTextProxy = textProxy;
        mEditTextProxy = editTextProxy;
    }

    /**
     * Measure a text based component for display.
     *
     * @param componentType Component type to determine property access to the text DOM
     * @param widthDp       Horizontal pixel width dimension as imposed by the parent.
     * @param widthMode     Horizontal width requirements as imposed by the parent.
     * @param heightDp      Vertical pixel width dimension as imposed by the parent.
     * @param heightMode    Vertical width requirements as imposed by the parent.
     * @return pixel based measurement results.
     */
    public float[] measure(String visualHash, ComponentType componentType,
                           float widthDp, MeasureMode widthMode,
                           float heightDp, MeasureMode heightMode) {
        mAplTrace.startTrace(TracePoint.TEXT_MEASURE);
        long start = 0;
        // track time and frequency

        // Any text measurements that occur after initial render are meaningless for tracking time to first frame.
        // If at some point we want to track successive measures we should be specific about their impact on performance
        // as a long-running document could accrue many of these without any performance concerns.
        if (mIsInitialRenderPass) {
            start = SystemClock.elapsedRealtime();
            mTelemetry.incrementCount(cMeasureText);
        }

        final float[] measurement = transformAndMeasure(componentType, widthDp, heightDp, widthMode, heightMode);
        if (mIsInitialRenderPass) {
            long duration = SystemClock.elapsedRealtime() - start;
            mTelemetry.incrementCount(cMeasureTextTotalTime, (int) duration);
        }

        mAplTrace.endTrace();
        return measurement;
    }

    /**
     * Transform DisplayPixel measurements to Pixel for Android Measurement.
     *
     * @param componentType Component type to determine property access to the text DOM
     * @param widthDp       Horizontal pixel width dimension as imposed by the parent.
     * @param widthMode     Horizontal width requirements as imposed by the parent.
     * @param heightDp      Vertical pixel width dimension as imposed by the parent.
     * @param heightMode    Vertical width requirements as imposed by the parent.
     * @return pixel based measurement results.
     */
    @VisibleForTesting
    private float[] transformAndMeasure(ComponentType componentType, float widthDp,
                                        float heightDp, MeasureMode widthMode, MeasureMode heightMode) {

        // Android internals of Text view are pixel based.
        final float widthPx = mMetricsTransform.toViewhost(widthDp);
        final float heightPx = mMetricsTransform.toViewhost(heightDp);

        if (shouldSkipLayoutPass(widthPx, heightPx, widthMode, mTextProxy.getDisplay())) {
            // Performance optimization: do not build layouts for text components with Display.kDisplayNone,
            // as this equals to View.GONE and does not affect layout pass. Also do not measure
            // if both requested width and height are Exactly or AtMost 0.
            mMeasuredWidthPx = Math.round(widthPx);
            mMeasuredHeightPx = Math.round(heightPx);
        } else {
            // Detailed Measurement
            measureTextContent(componentType, Math.round(widthPx), widthMode,
                    Math.round(heightPx), heightMode);
        }

        // return results
        final float[] measurement = new float[]{
                mMetricsTransform.toCore(mMeasuredWidthPx),
                mMetricsTransform.toCore(mMeasuredHeightPx)};

        return measurement;
    }

    /**
     * Measure the Text Component. This method builds a StaticLayout based on the Component
     * properties and saves it in {@link TextLayoutCache}.
     *
     * @param type       Component type to determine property access to the text DOM
     * @param widthPx    Horizontal pixel width dimension as imposed by the parent.
     * @param widthMode  Horizontal width requirements as imposed by the parent.
     * @param heightPx   Vertical pixel width dimension as imposed by the parent.
     * @param heightMode Vertical width requirements as imposed by the parent.
     */
    private void measureTextContent(
            final ComponentType type,
            final int widthPx,
            @NonNull final MeasureMode widthMode,
            final int heightPx,
            @NonNull final MeasureMode heightMode) {

        // In all cases for the width measureTextContent mode (undefined, at most, exactly) the text layout
        // should match or fit within the width dimension
        final Layout textLayout = type == ComponentType.kComponentTypeText
                ? mTextLayoutFactory.getOrCreateTextLayout(mVersionCode, mTextProxy,
                widthPx, widthMode, heightPx, null)
                : mTextLayoutFactory.createEditTextLayout(mVersionCode, mEditTextProxy,
                widthPx, heightPx);


        // Width mode is already taken into account when building the layout. No need to do extra magic.
        mMeasuredWidthPx = textLayout.getWidth();

        // The measured height is derived from the calculated layout, and adjusted / overridden
        // based on the measureTextContent mode
        switch (heightMode) {
            case Undefined:
                // use the layout preferred height
                mMeasuredHeightPx = textLayout.getHeight();
                break;
            case AtMost:
                // use the measured height, unless it exceeds the limit
                mMeasuredHeightPx = Math.min(Math.round(heightPx), textLayout.getHeight());
                break;
            case Exactly:
                // force the height as directed by the parent
                mMeasuredHeightPx = Math.round(heightPx);
                break;
        }
    }

    private boolean shouldSkipLayoutPass(float widthPx, float heightPx,
                                 MeasureMode widthMode, Display display) {
        if (display == Display.kDisplayNone) {
            return true;
        }

        final boolean widthModeExactlyOrAtMost = (widthMode == TextMeasure.MeasureMode.Exactly ||
                widthMode == MeasureMode.AtMost);
        return widthPx == 0 && heightPx == 0 && widthModeExactlyOrAtMost;
    }

}
