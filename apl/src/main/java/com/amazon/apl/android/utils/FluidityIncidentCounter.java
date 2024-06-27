/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.annotation.TargetApi;
import android.os.Build;

import com.amazon.apl.android.providers.ITelemetryProvider;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Counts the number of times fluidity incidents occur as per runtime provided configuration
 * 1. Maintains a sliding window of frame times and key fluidity metrics
 * 2. Emits incident counter metric whenever the fluidity metric breaches threshold
 * The fluidity metric of interest is UPS (User Perceived Smoothness).
 * Ideal UPS should be 1.0
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class FluidityIncidentCounter {
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
    private final int mMetricId;
    private double mRollingSum;
    private double mRollingSquaredSum;
    private boolean mActiveIncident;

    public FluidityIncidentCounter(int windowSize, ITelemetryProvider telemetryProvider, double thresholdUPS, double displayRefreshTimeMs, int metricId) {
        mWindowSize = windowSize;
        mTelemetryProvider = telemetryProvider;
        mThresholdUPS = thresholdUPS;
        mMetricId = metricId;
        mDisplayRefreshTimeMs = displayRefreshTimeMs;
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
     * @param frameStat
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

        if (rollingUps > mThresholdUPS && !mActiveIncident) {
            mTelemetryProvider.incrementCount(mMetricId);
            // The system is experiencing low fluidity
            mActiveIncident = true;
        } else if (mActiveIncident && rollingUps <= mThresholdUPS) {
            // The system recovered from low fluidity
            mActiveIncident = false;
        }
    }
}
