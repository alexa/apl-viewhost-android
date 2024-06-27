/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazon.apl.android.providers.ITelemetryProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FluidityIncidentCounterTest {
    private FluidityIncidentCounter fluidityIncidentCounter;

    @Mock
    private ITelemetryProvider telemetryProviderMock;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        int windowSize = 10;
        double thresholdUPS = 1.2;
        double displayRefreshTimeMs = 16.67; // For 60 Hz refresh rate
        int metricId = 123;
        fluidityIncidentCounter = new FluidityIncidentCounter(windowSize, telemetryProviderMock, thresholdUPS, displayRefreshTimeMs, metricId);
    }

    @Test
    public void testAddFrameStat_WithinThreshold() {
        // Act
        simulateFrameTimes(16_670_000); // Simulating 60 FPS

        // Assert
        verify(telemetryProviderMock, never()).incrementCount(anyInt());
    }

    @Test
    public void testAddFrameStat_OutsideThreshold() {
        // Act
        simulateFrameTimes(33_340_000); // Simulating 30 FPS

        // Assert
        verify(telemetryProviderMock).incrementCount(123);
    }

    @Test
    public void testAddFrameStat_threshold_breach_emits_incident() {
        // Act
        simulateFrameTimes(16_670_000); // Simulating 60 FPS
        // Act
        simulateFrameTimes(33_340_000); // Simulating 30 FPS, threshold breached once
        // Act
        simulateFrameTimes(16_670_000); // Simulating 60 FPS
        // Act
        simulateFrameTimes(33_340_000); // Simulating 30 FPS, threshold breached once more

        // Assert incident is reported twice
        verify(telemetryProviderMock, times(2)).incrementCount(123);
    }

    private void simulateFrameTimes(long elapsedTime) {
        long startTime = System.nanoTime();
        FrameStat frameStat = new FrameStat(startTime, startTime + elapsedTime);
        for (int i = 0; i < 10; i++) {
            fluidityIncidentCounter.addFrameStat(frameStat);
            startTime += elapsedTime;
            frameStat = new FrameStat(startTime, startTime + elapsedTime);
        }
    }
}
