/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.providers.ITelemetryProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Counts the number of times fluidity incidents occur as per runtime provided configuration
 * 1. Maintains a sliding window of frame times and key fluidity metrics
 * 2. Emits incident counter metric whenever the fluidity metric breaches threshold
 * The fluidity metric of interest is UPS (User Perceived Smoothness).
 * Ideal UPS should be 1.0
 */
public class FluidityIncidentReporter {
    private static final String TAG = "FluidityIncident";
    /**
     * This list must not contain more than mWindowSize elements at any point of time
     */
    private FrameStat mLastFrameStat;
    private final Deque<Double> mFrameTimes = new ArrayDeque<>();
    private final int mWindowSize;
    private final double mDisplayRefreshTimeMs;
    private final ITelemetryProvider mTelemetryProvider;
    private final double mThresholdUPS;
    private final double mMinimumDurationMs;
    private final int mMetricId;
    private double mRollingSum;
    private double mRollingSquaredSum;
    private boolean mActiveIncident;
    private double mTm95UPS;
    private long mTimeWhenUpsRoseAboveThreshold;
    // TODO: Remove maxUPS metric once runtime configuration scheme is updated to track TM95
    static final String MAX_INCIDENT_UPS = "maxUPS";
    static final String TM95_INCIDENT_UPS = "tm95UPS";
    private final int mTm95UPSMetricId;
    private final int mMaxUPSMetricId;
    private boolean mShouldEmitIncidentReportedEvent;
    private final List<FrameStat> mIncidentReportedFrameStats = new ArrayList<>();
    private final List<Double> mIncidentReportedUpsValues = new ArrayList<>();
    // For now this is a simple counter
    private int mCurrentIncidentId;
    static final String FLUIDITY_SUCCESS_INDICATOR = "fluiditySuccessIndicator";
    private final int mFluiditySuccessIndicator;
    private final TDigest tDigest;

    public FluidityIncidentReporter(int windowSize, ITelemetryProvider telemetryProvider,
                                    double thresholdUPS, double displayRefreshTimeMs,
                                    double minimumDurationMs, int metricId) {
        mWindowSize = windowSize;
        mTelemetryProvider = telemetryProvider;
        mThresholdUPS = thresholdUPS < 1 ? 1 : thresholdUPS;
        mTm95UPS = 0.0; // Initializing to 0 with the intention that a 0 emitted value for this would mean something wrong
        mMetricId = metricId;
        mDisplayRefreshTimeMs = displayRefreshTimeMs;
        mMinimumDurationMs = minimumDurationMs;
        mTm95UPSMetricId = telemetryProvider.createMetricId(APL_DOMAIN, TM95_INCIDENT_UPS, ITelemetryProvider.Type.COUNTER);
        mMaxUPSMetricId = telemetryProvider.createMetricId(APL_DOMAIN, MAX_INCIDENT_UPS, ITelemetryProvider.Type.COUNTER);
        mFluiditySuccessIndicator = telemetryProvider.createMetricId(APL_DOMAIN, FLUIDITY_SUCCESS_INDICATOR, ITelemetryProvider.Type.COUNTER);
        tDigest = new TDigest(10);
    }

    /**
     * This method keeps updates the following metrics with each frame
     * Rolling window sum and rolling window mean are simple
     * The rolling standard deviation is calculated as follows:
     * Rolling Squared diff       = ∑(x[i] - x̄)^2
     *                            = ∑(x[i])^2 - (2 * x̄ * ∑x[i]) + ∑x̄^2
     * ∑(x[i])^2                  = Rolling squared sum + (incoming)^2 - (outgoing)^2
     * ∑x[i]                      = Rolling sum + incoming - outgoing
     * x̄ or mean                  = Rolling sum / Window size
     * ∑x̄^2                       = Window size times mean^2
     * Rolling Standard deviation = Square root of (Rolling squared diff / Window size)
     * @param frameStat {@link FrameStat}
     */
    public void addFrameStat(FrameStat frameStat) {
        if (mLastFrameStat == null) {
            mLastFrameStat = frameStat;
            return;
        }
        double numerator = frameStat.begin - mLastFrameStat.begin;
        mLastFrameStat = frameStat;
        double incomingFrameTime = numerator / 1000000.0d;
        // Frame calls are sometimes less than refresh time apart, this leads to higher sigma
        if (incomingFrameTime < mDisplayRefreshTimeMs) {
            incomingFrameTime = mDisplayRefreshTimeMs;
        }
        double outgoingFrameTime = 0.0d;
        if (mFrameTimes.size() == mWindowSize) {
            outgoingFrameTime = mFrameTimes.removeFirst();
        }
        mFrameTimes.addLast(incomingFrameTime);
        mRollingSum += incomingFrameTime - outgoingFrameTime;
        mRollingSquaredSum += (incomingFrameTime * incomingFrameTime) - (outgoingFrameTime * outgoingFrameTime);
        double rollingMean = mRollingSum / mFrameTimes.size();
        double rollingVariance = (mRollingSquaredSum / mFrameTimes.size())
                + ((mFrameTimes.size() * rollingMean * rollingMean) / mFrameTimes.size())
                - (2 * rollingMean * mRollingSum) / mFrameTimes.size();
        if (rollingVariance < 0.0d) {
            rollingVariance = 0.0d;
        }
        double rollingStandardDeviation = Math.sqrt(rollingVariance);
        double rollingUpfd = rollingMean + 2 * rollingStandardDeviation;
        double rollingUps = rollingUpfd / mDisplayRefreshTimeMs;
        // Likewise other rolling stats can be tracked irrespective of occurrence of incident
        // Record UPS per frame window using t-digest data structure for metric computation later on
        tDigest.add(rollingUps);
        if (rollingUps > mThresholdUPS && mTimeWhenUpsRoseAboveThreshold == 0.0 && !mActiveIncident) {
            // The system just started experiencing low fluidity
            mTimeWhenUpsRoseAboveThreshold = frameStat.begin;
        }
        if (rollingUps > mThresholdUPS && !mActiveIncident) {
            mIncidentReportedFrameStats.add(frameStat);
            mIncidentReportedUpsValues.add(rollingUps);
        }
        if (rollingUps > mThresholdUPS
                && !mActiveIncident
                && mTimeWhenUpsRoseAboveThreshold != 0
                && (frameStat.begin - mTimeWhenUpsRoseAboveThreshold) / 1000000.0 > mMinimumDurationMs) {
            mCurrentIncidentId++;
            mTelemetryProvider.incrementCount(mMetricId);
            // The system is experiencing low fluidity since some time
            mActiveIncident = true;
            mShouldEmitIncidentReportedEvent = true;
        } else if (rollingUps <= mThresholdUPS) {
            // The system recovered from low fluidity
            mTimeWhenUpsRoseAboveThreshold = 0;
            mActiveIncident = false;
            mShouldEmitIncidentReportedEvent = false;
        }
    }

    /**
     * Emits the necessary indicator metrics
     */
    public void emitFluidityMetrics() {
        // upper bound is exclusive
        mTm95UPS = tDigest.trimmedMean(0.0, 0.96);
        mTelemetryProvider.incrementCount(mTm95UPSMetricId, mTm95UPS);
        mTelemetryProvider.incrementCount(mMaxUPSMetricId, mTm95UPS);
        mTelemetryProvider.incrementCount(mFluiditySuccessIndicator);
        if (mTm95UPS > mThresholdUPS) {
            mTelemetryProvider.fail(mFluiditySuccessIndicator);
        }
    }

    public int getCurrentIncidentId() {
        return mCurrentIncidentId;
    }

    public boolean getAndResetShouldReportEvent() {
        if (mShouldEmitIncidentReportedEvent) {
            mShouldEmitIncidentReportedEvent = false;
            return true;
        }
        return false;
    }

    public FrameStat[] getAndResetFrameIncidentReportedFrameStats() {
        FrameStat[] frameStats = new FrameStat[mIncidentReportedFrameStats.size()];
        frameStats = mIncidentReportedFrameStats.toArray(frameStats);
        mIncidentReportedFrameStats.clear();
        return frameStats;
    }

    public Double[] getAndResetFrameIncidentReportedUpsValue() {
        Double[] upsValues = new Double[mIncidentReportedUpsValues.size()];
        upsValues = mIncidentReportedUpsValues.toArray(upsValues);
        mIncidentReportedUpsValues.clear();
        return upsValues;
    }

    /**
     * Constructs and returns a JSONObject containing information about the document
     * state when the fluidity incident occurred fetched from Core.
     *
     * @return JSONObject containing the incident details.
     */
    public JSONObject getFluidityIncidentDetails(RootContext rootContext) {
        JSONObject details = new JSONObject();
        JSONArray documentState;

        if (rootContext == null) {
            Log.w(TAG, "Invalid RootContext, returning an empty details object");
            return details;
        }

        try {
            documentState = new JSONArray(rootContext.serializeDocumentState());
            details.put("documentState", documentState);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to construct fluidityEvent, returning an empty details object");
        }

        return details;
    }
}
