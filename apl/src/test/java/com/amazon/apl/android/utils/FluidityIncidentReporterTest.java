/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.utils.FluidityIncidentReporter.FLUIDITY_SUCCESS_INDICATOR;
import static com.amazon.apl.android.utils.FluidityIncidentReporter.MAX_INCIDENT_UPS;
import static com.amazon.apl.android.utils.FluidityIncidentReporter.TM95_INCIDENT_UPS;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.providers.ITelemetryProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FluidityIncidentReporterTest {
    private FluidityIncidentReporter fluidityIncidentReporter;

    @Mock
    private ITelemetryProvider telemetryProviderMock;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        int windowSize = 10;
        double thresholdUPS = 1.2;
        double displayRefreshTimeMs = 16.67; // For 60 Hz refresh rate
        double minimumDurationMs = 100;
        int metricId = 123;
        when(telemetryProviderMock.createMetricId(APL_DOMAIN, TM95_INCIDENT_UPS, ITelemetryProvider.Type.COUNTER)).thenReturn(4);
        when(telemetryProviderMock.createMetricId(APL_DOMAIN, FLUIDITY_SUCCESS_INDICATOR, ITelemetryProvider.Type.COUNTER)).thenReturn(5);
        when(telemetryProviderMock.createMetricId(APL_DOMAIN, MAX_INCIDENT_UPS, ITelemetryProvider.Type.COUNTER)).thenReturn(6);
        fluidityIncidentReporter = new FluidityIncidentReporter(windowSize, telemetryProviderMock, thresholdUPS, displayRefreshTimeMs, minimumDurationMs, metricId);
    }

    @Test
    public void testAddFrameStat_WithinThreshold() {
        // Act
        simulateFrameTimes(16_670_000, 10); // Simulating 60 FPS for 10 frames

        // Assert
        verify(telemetryProviderMock, never()).incrementCount(anyInt());

        //verify increment count called always
        fluidityIncidentReporter.emitFluidityMetrics();
        verify(telemetryProviderMock).incrementCount(4, 1.0d);
        verify(telemetryProviderMock).incrementCount(5);
        verify(telemetryProviderMock).incrementCount(6, 1.0d);
    }

    @Test
    public void testAddFrameStat_OutsideThreshold() {
        // Act
        simulateFrameTimes(33_340_000, 10); // Simulating 30 FPS for 10 frames

        // Assert
        verify(telemetryProviderMock).incrementCount(123);
        fluidityIncidentReporter.emitFluidityMetrics();
        verify(telemetryProviderMock).incrementCount(4, 2.0d);
        verify(telemetryProviderMock).incrementCount(5);
        verify(telemetryProviderMock).incrementCount(6, 2.0d);
        verify(telemetryProviderMock).fail(5);
    }

    @Test
    public void testAddFrameStat_threshold_breach_emits_incident() {
        // Act
        simulateFrameTimes(16_670_000, 10); // Simulating 60 FPS for 10 frames
        // Act
        simulateFrameTimes(33_340_000, 10); // Simulating 30 FPS for 10 frames, threshold breached once
        // Act
        simulateFrameTimes(16_670_000, 10); // Simulating 60 FPS for 10 frames
        // Act
        simulateFrameTimes(33_340_000, 10); // Simulating 30 FPS for 10 frames, threshold breached once more

        // Assert incident is reported twice
        verify(telemetryProviderMock, times(2)).incrementCount(123);
    }

    @Test
    public void testAddFrameStat_boundary_event_emitted() {
        // A few normal Vsync rate frames
        simulateFrameTimes(16_670_000, 10); // Simulating 60 FPS for 10 frames
        // A few delayed frames
        simulateFrameTimes(33_340_000, 4); // Simulating 30 FPS for 4 frames, threshold breached once but not long enough yet to breach minimumDurationMs
        // Assert incident is not reported yet
        verify(telemetryProviderMock, times(0)).incrementCount(123);
        assertFalse(fluidityIncidentReporter.getAndResetShouldReportEvent());
        assertEquals(3, fluidityIncidentReporter.getAndResetFrameIncidentReportedFrameStats().length);
        assertEquals(3, fluidityIncidentReporter.getAndResetFrameIncidentReportedUpsValue().length);
        // A few more delayed frames
        simulateFrameTimes(16_670_000, 9); // Simulating 60 FPS for 10 frames, now enough time has passed beyond minimumDurationMs
        // Assert incident is reported once
        verify(telemetryProviderMock, times(1)).incrementCount(123);
        // FrameStats accumulate
        assertTrue(fluidityIncidentReporter.getAndResetShouldReportEvent());
        assertEquals(1, fluidityIncidentReporter.getCurrentIncidentId());
        assertEquals(9, fluidityIncidentReporter.getAndResetFrameIncidentReportedFrameStats().length);
        assertEquals(9, fluidityIncidentReporter.getAndResetFrameIncidentReportedUpsValue().length);
        // Move further
        simulateFrameTimes(16_670_000, 10); // Simulating 60 FPS for 10 frames, now enough time has passed beyond minimumDurationMs
        verify(telemetryProviderMock, times(1)).incrementCount(123);
        // Every call to getAndReset once incident is over resets the internal storage structures, no unbounded growth
        assertFalse(fluidityIncidentReporter.getAndResetShouldReportEvent());
        assertEquals(0, fluidityIncidentReporter.getAndResetFrameIncidentReportedFrameStats().length);
        assertEquals(0, fluidityIncidentReporter.getAndResetFrameIncidentReportedUpsValue().length);
    }

    @Test
    public void test_getFluidityIncidentDetails_null_rootContext() {
        JSONObject details = fluidityIncidentReporter.getFluidityIncidentDetails(null);

        assertNotNull(details);
        assertEquals(0, details.length());
    }

    @Test
    public void test_getFluidityIncidentDetails_with_rootContext() throws JSONException {
        String jsonString = "[{\"document\":\"main\",\"actions\":[{\"component\":{\"provenance\":\"_main/mainTemplate/items/items/0\",\"targetComponentType\":\"ScrollView\",\"targetId\":\"myScrollView\"},\"actionHint\":\"Scrolling\"}]}]";
        JSONArray actionsArray = new JSONArray(jsonString);
        JSONObject expected = new JSONObject();
        expected.put("documentState", actionsArray);
        RootContext rootContext = mock(RootContext.class);
        when(rootContext.serializeDocumentState()).thenReturn(jsonString);

        JSONObject details = fluidityIncidentReporter.getFluidityIncidentDetails(rootContext);

        assertEquals(details.toString(), expected.toString());
    }

    private void simulateFrameTimes(long elapsedTime, int frameCount) {
        long startTime = System.nanoTime();
        FrameStat frameStat = new FrameStat(startTime, startTime + elapsedTime);
        for (int i = 0; i < frameCount; i++) {
            fluidityIncidentReporter.addFrameStat(frameStat);
            startTime += elapsedTime;
            frameStat = new FrameStat(startTime, startTime + elapsedTime);
        }
    }
}
