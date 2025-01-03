/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;
import static com.amazon.apl.enums.ComponentType.kComponentTypeText;

import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.FrameStat;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;

import java.util.Calendar;
import java.util.Date;

public class RootContextTest extends AbstractDocUnitTest {

    private final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Frame\",\n" +
            "            \"id\": \"frame\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"backgroundColor\": \"green\"\n" +
            "        }\n" +
            "    }\n" +
            "}";


    private static final String SIMPLE_DOC_WITH_PARAMETER = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.3\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"data1\"" +
            "    ]," +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void test_OptionalParameters() {
        Content content = Content.create(SIMPLE_DOC_WITH_PARAMETER,
                null,
                null,
                new Session(),
                null,
                true,
                new Handler(Looper.getMainLooper()));

        Assert.assertFalse(content.isWaiting());
        Assert.assertFalse(content.isReady());

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        mAPLPresenter = mock(IAPLViewPresenter.class);
        when(mAPLPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);

        RootContext.create(metrics, content, buildRootConfig(), APLOptions.builder().build(), mAPLPresenter, mMetricsRecorder, mFluidityIncidentReporter);
    }

    @Test
    public void test_InflateDoesNotUpdateContext() {
        IVisualContextListener contextListener = mock(IVisualContextListener.class);

        loadDocument(DOC, APLOptions
                .builder()
                .visualContextListener(contextListener)
                .build());

        verify(contextListener, never()).onVisualContextUpdate(any(JSONObject.class));
    }

    @Test
    public void test_timersInitializedDuringRestore() {
        ITelemetryProvider telemetryMock = Mockito.mock(ITelemetryProvider.class);
        IMetricsRecorder metricsRecorderMock = Mockito.mock(IMetricsRecorder.class);
        APLOptions aplOptions = APLOptions.builder().telemetryProvider(telemetryMock).build();
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();

        loadDocument(DOC, aplOptions, metrics);

        DocumentState documentState = new DocumentState(mRootContext, mContent, 0);
        reset(telemetryMock);

        RootContext.createFromCachedDocumentState(documentState, mAPLPresenter, mFluidityIncidentReporter);
        verify(telemetryMock, times(1)).createMetricId(APL_DOMAIN, RootContext.METRIC_REINFLATE, TIMER);
    }

    @Test
    public void test_pauseAndResume() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();

        APLOptions aplOptions = APLOptions.builder().build();

        loadDocument(DOC, aplOptions, metrics);

        InOrder inOrder = Mockito.inOrder(mRootContext, mAPLPresenter);

        // Document is running, so it can be paused now
        mRootContext.pauseDocument();
        inOrder.verify(mAPLPresenter).onDocumentPaused();

        // Repeating the pause has no effect
        mRootContext.pauseDocument();
        inOrder.verify(mAPLPresenter, never()).onDocumentPaused();

        // But you can resume it
        mRootContext.resumeDocument();
        inOrder.verify(mAPLPresenter).onDocumentResumed();

        // Repeating the resume has no effect
        mRootContext.resumeDocument();
        inOrder.verify(mAPLPresenter, never()).onDocumentResumed();
    }

    @Test
    public void test_reportSuccess() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();
        UserPerceivedFatalReporter userPerceivedFatalReporter = Mockito.mock(UserPerceivedFatalReporter.class);

        APLOptions aplOptions = APLOptions.builder().build();

        loadDocument(DOC, aplOptions, metrics, userPerceivedFatalReporter);

        InOrder inOrder = Mockito.inOrder(mRootContext, mAPLPresenter);

        // Document is running, so it can be finished now
        mRootContext.finishDocument();
        verify(mFluidityIncidentReporter).emitFluidityMetrics();
        verify(userPerceivedFatalReporter).reportSuccess();
    }

    @Test
    public void testFinishDocument_whenCalledFromMainThread_shouldExecuteRunnableImmediately() {
        ShadowLooper.pauseMainLooper();
        loadDocument();
        mRootContext.finishDocument();
        // Runnable is executed immediately, so does not wait for another loop on main looper
        verify(mAPLPresenter).onDocumentFinish();
        ShadowLooper.idleMainLooper();
    }

    @Test
    public void testFinishDocument_whenCalledFromBackgroundThread_shouldPostRunnableToMainHandler() {
        // Pause Main looper
        ShadowLooper.pauseMainLooper();
        loadDocument();
        // Finish document on background thread
        Thread backgroundThread = new Thread(() -> {
            Looper.prepare();
            mRootContext.finishDocument();
            Looper.loop();
        });
        backgroundThread.start();
        // Loop the main looper
        ShadowLooper.idleMainLooper();
        // Since the runnable was posted on main thread, the following verification now passes
        verify(mAPLPresenter).onDocumentFinish();
    }

    private final String TIME_DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"text\",\n" +
            "            \"text\": \"%s\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    public void test_updateTime_localTime() {
        Date before = new Date();
        loadDocument(String.format(TIME_DOC, "${localTime}"));
        Date after = new Date();
        // Local time should be the current time in the local timezone
        Component component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        Calendar now = Calendar.getInstance();
        long offset = now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET);
        assertTrue(before.getTime() <= Long.parseLong(((Text) component).getText()) - offset);
        assertTrue(Long.parseLong(((Text) component).getText()) - offset <= after.getTime());
    }

    @Test
    public void test_updateTime_utcTime() {
        // UTC time should be the utc time
        Date before = new Date();
        loadDocument(String.format(TIME_DOC, "${utcTime}"));
        Date after = new Date();
        Component component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        assertTrue(before.getTime() <= Long.parseLong(((Text) component).getText()));
        assertTrue(Long.parseLong(((Text) component).getText()) <= after.getTime());
    }

    @Test
    public void test_updateTime_localTime_elapsedTime() {
        // Elapsed time should be 0 initially
        loadDocument(String.format(TIME_DOC, "${elapsedTime}"));
        Component component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        assertEquals("0", ((Text)component).getText());
        update(500);
        // Elapsed time should be updated to 500 now
        component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        assertEquals("500", ((Text)component).getText());
    }

    @Test
    public void testReportFluidityIncidentWhenEnabled() throws JSONException {
        // Arrange
        FrameStat frameStat1 = new FrameStat(0, 1);
        FrameStat frameStat2 = new FrameStat(1, 2);
        FrameStat[] reportedFrameStats = new FrameStat[]{frameStat1, frameStat2};
        Double[] reportedUpsValues = new Double[]{1.5, 2.0};
        JSONObject fluidityIncidentDetails = new JSONObject();
        fluidityIncidentDetails.put("documentDetails", new JSONArray());

        when(mFluidityIncidentReporter.getAndResetShouldReportEvent()).thenReturn(true);
        when(mFluidityIncidentReporter.getCurrentIncidentId()).thenReturn(123);
        when(mFluidityIncidentReporter.getAndResetFrameIncidentReportedFrameStats()).thenReturn(reportedFrameStats);
        when(mFluidityIncidentReporter.getAndResetFrameIncidentReportedUpsValue()).thenReturn(reportedUpsValues);
        when(mFluidityIncidentReporter.getFluidityIncidentDetails(any(RootContext.class))).thenReturn(fluidityIncidentDetails);

        // Act
        setFrameMetricsEnabledState(true);
        loadDocument(String.format(TIME_DOC, "${elapsedTime}")); // This ticks once

        // Assert
        verify(mFluidityIncidentReporter).getAndResetShouldReportEvent();
        verify(mAPLPresenter).emitFluidityIncident(123, reportedFrameStats, reportedUpsValues, fluidityIncidentDetails);
    }

    @Test
    public void testReportFluidityIncidentWhenDisabled() throws JSONException {
        // Act
        setFrameMetricsEnabledState(false);
        loadDocument(String.format(TIME_DOC, "${elapsedTime}")); // This ticks once

        // Assert
        verify(mFluidityIncidentReporter, times(0)).getAndResetShouldReportEvent();
        verify(mAPLPresenter, times(0)).emitFluidityIncident(anyInt(), any(), any(), any());
    }

    private void loadDocument() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();
        UserPerceivedFatalReporter userPerceivedFatalReporter = Mockito.mock(UserPerceivedFatalReporter.class);

        APLOptions aplOptions = APLOptions.builder().build();

        loadDocument(DOC, aplOptions, metrics, userPerceivedFatalReporter);
    }
}
