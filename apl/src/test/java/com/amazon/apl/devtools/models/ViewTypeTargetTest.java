/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.devtools.models;

import static com.amazon.apl.devtools.enums.EventMethod.FRAMEMETRICS_INCIDENT_REPORTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;
import com.amazon.apl.android.utils.FrameStat;
import com.amazon.apl.android.utils.MetricInfo;
import com.amazon.apl.devtools.enums.ViewState;
import com.amazon.apl.devtools.models.frameMetrics.FrameIncidentReportedEvent;
import com.amazon.apl.devtools.models.log.LogEntry;
import com.amazon.apl.devtools.models.log.LogEntryAddedEvent;

import com.amazon.apl.devtools.models.performance.IMetricsService;
import com.amazon.apl.devtools.models.performance.PerformanceMetricsEvent;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class ViewTypeTargetTest {
    @Mock
    private Handler mockHandler;
    @Mock
    private Session mockSession;

    private ArgumentCaptor<Runnable> mHandlerArgumentCaptor;
    private ViewTypeTarget viewTypeTarget;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mHandlerArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(mockSession.isLogEnabled()).thenReturn(true);
        when(mockSession.getSessionId()).thenReturn("session1");
        viewTypeTarget = new ViewTypeTarget(mockHandler);
        viewTypeTarget.registerSession(mockSession);
    }

    @Test
    public void testOnRegisterSessionGetsNotifiedOnLatestViewState() {
        // Set up
        verify(mockHandler).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();
        Session mockSession2 = mock(Session.class);
        when(mockSession2.getSessionId()).thenReturn("session2");

        // Test
        viewTypeTarget.registerSession(mockSession2);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        // Verify
        verify(mockSession, times(1)).sendEvent(any());
        verify(mockSession2, times(1)).sendEvent(any());
    }

    @Test
    public void testPerformanceMetricsEventNoEventsAreSentWhenResultsAreNull() {
        viewTypeTarget.onViewStateChange(ViewState.READY, 0.0);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        verify(mockSession, times(0)).sendEvent(any(PerformanceMetricsEvent.class));
    }

    @Test
    public void testPerformanceMetricsEventsPrioritizeLoggingTelementryProvider() throws JSONException {
        // Setup
        ArgumentCaptor<PerformanceMetricsEvent> eventArgumentCaptor = ArgumentCaptor.forClass(PerformanceMetricsEvent.class);
        APLOptions options = mock(APLOptions.class);
        LoggingTelemetryProvider metricsProvider = mock(LoggingTelemetryProvider.class);
        MetricInfo metric = new MetricInfo("testName", 0.0);
        List<MetricInfo> expectedResult = Arrays.asList(new MetricInfo[]{metric});

        IMetricsService metricsService = mock(IMetricsService.class);

        when(options.getTelemetryProvider()).thenReturn(metricsProvider);
        when(metricsProvider.getPerformanceMetrics()).thenReturn(expectedResult);
        when(mockSession.isPerformanceEnabled()).thenReturn(true);

        // Test
        viewTypeTarget.setMetricsRetriever(metricsService);
        viewTypeTarget.setAPLOptions(options);
        viewTypeTarget.onViewStateChange(ViewState.READY, 0.0);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getAllValues().get(1).run();

        // Verify
        verify(mockSession, times(2)).sendEvent(eventArgumentCaptor.capture());
        verifyNoInteractions(metricsService);

        PerformanceMetricsEvent event = eventArgumentCaptor.getAllValues().get(1);
        JSONObject reportedMetric = ((JSONArray) ((JSONObject) event.toJSONObject().get("params")).get("metrics")).getJSONObject(0);
        assertEquals(metric.getName(), reportedMetric.get("name"));
    }

    @Test
    public void testPerformanceMetricsEventEmitMetricsUsingMetricsService() throws JSONException {
        ArgumentCaptor<PerformanceMetricsEvent> eventArgumentCaptor = ArgumentCaptor.forClass(PerformanceMetricsEvent.class);
        IMetricsService metricsService = mock(IMetricsService.class);
        MetricInfo metric = new MetricInfo("testName", 0.0);
        List<MetricInfo> expectedResult = Arrays.asList(new MetricInfo[]{metric});

        when(metricsService.retrieveMetrics()).thenReturn(expectedResult);
        when(mockSession.isPerformanceEnabled()).thenReturn(true);

        viewTypeTarget.setMetricsRetriever(metricsService);
        viewTypeTarget.onViewStateChange(ViewState.READY, 0.0);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getAllValues().get(1).run();

        // Verify
        verify(mockSession, times(2)).sendEvent(eventArgumentCaptor.capture());

        PerformanceMetricsEvent event = eventArgumentCaptor.getAllValues().get(1);
        JSONObject reportedMetric = ((JSONArray) ((JSONObject) event.toJSONObject().get("params")).get("metrics")).getJSONObject(0);
        assertEquals(metric.getName(), reportedMetric.get("name"));
    }

    @Test
    public void testOnLogEntryAdded() {
        com.amazon.apl.android.Session.LogEntryLevel level = com.amazon.apl.android.Session.LogEntryLevel.INFO;
        com.amazon.apl.android.Session.LogEntrySource source = com.amazon.apl.android.Session.LogEntrySource.COMMAND;
        String messageText = "Test log message";
        double timestamp = 123456789.0;
        Object[] arguments = {"arg1", "arg2"};

        viewTypeTarget.onLogEntryAdded(level, source, messageText, timestamp, arguments);

        // Verify that the logEntries list is updated
        List<LogEntry> logEntries = viewTypeTarget.getLogEntries();
        assertEquals(1, logEntries.size());
        assertEquals(level, logEntries.get(0).getLevel());
        assertEquals(source, logEntries.get(0).getSource());
        assertEquals(messageText, logEntries.get(0).getText());
        assertEquals(timestamp, logEntries.get(0).getTimestamp(), 0.0);

        // Verify that the sendEvent method is called for the registered session
        verify(mockSession, times(1)).sendEvent(any(LogEntryAddedEvent.class));
    }

    @Test
    public void testClearLog() {
        com.amazon.apl.android.Session.LogEntryLevel level = com.amazon.apl.android.Session.LogEntryLevel.INFO;
        com.amazon.apl.android.Session.LogEntrySource source = com.amazon.apl.android.Session.LogEntrySource.COMMAND;
        String messageText = "Test log message";
        double timestamp = 123456789.0;
        Object[] arguments = {"arg1", "arg2"};
        LogEntry logEntry = new LogEntry(level, source, messageText, timestamp, arguments);
        viewTypeTarget.getLogEntries().add(logEntry);

        viewTypeTarget.clearLog();

        // Verify that the logEntries list is cleared
        assertEquals(0, viewTypeTarget.getLogEntries().size());
    }

    @Test
    public void testFrameMetricsEvent() throws JSONException {
        FrameStat frameStat1 = new FrameStat(1, 2);
        FrameStat frameStat2 = new FrameStat(1, 2);
        FrameStat[] frameStats = new FrameStat[]{frameStat1, frameStat2};
        Double[] upsValues = new Double[]{1.5, 2.0};
        reset(mockHandler);
        viewTypeTarget.onFrameIncidentReported(1, frameStats, upsValues, new JSONObject());
        verify(mockHandler).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();
        ArgumentCaptor<FrameIncidentReportedEvent> eventCaptor = ArgumentCaptor.forClass(FrameIncidentReportedEvent.class);
        verify(mockSession).sendEvent(eventCaptor.capture());
        FrameIncidentReportedEvent event = eventCaptor.getValue();
        assertEquals(FRAMEMETRICS_INCIDENT_REPORTED, event.getMethod());
        JSONObject eventJsonParams = event.toJSONObject().getJSONObject("params");
        assertEquals(1, eventJsonParams.get("incidentId"));
        assertEquals(2, ((JSONArray) eventJsonParams.get("framestats")).length());
        assertEquals(2, ((JSONArray) eventJsonParams.get("upsValues")).length());
    }
}

