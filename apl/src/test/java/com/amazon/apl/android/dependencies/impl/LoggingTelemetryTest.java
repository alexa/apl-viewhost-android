/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies.impl;

import static android.util.Log.INFO;

import com.amazon.apl.android.providers.ITelemetryProvider.Type;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.utils.MetricInfo;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class LoggingTelemetryTest extends ViewhostRobolectricTest {

    private final static String DOMAIN = "DOMAIN";
    private final static String METRIC_COUNTER = "METRIC_COUNTER";
    private final static String METRIC_TIMER = "METRIC_TIMER";
    private final static String METRIC_TIMER_REPORT = "METRIC_TIMER_REPORT";
    private final static long TWO_MILI = TimeUnit.MILLISECONDS.toNanos(2);
    private final static long TWO_SEC = TimeUnit.SECONDS.toNanos(2);
    private final static String LOG_TAG = "TelemetryReport";

    private LoggingTelemetryProvider tProvider;
    private int tCounter;
    private int tTimer;
    private int rTimer;

    @Before
    public void before() {
        tProvider = spy(new LoggingTelemetryProvider());
        doAnswer(invocation -> System.nanoTime()).when(tProvider).realtimeNanos();
        tCounter = tProvider.createMetricId(DOMAIN, METRIC_COUNTER, Type.COUNTER);
        tTimer = tProvider.createMetricId(DOMAIN, METRIC_TIMER, Type.TIMER);
        rTimer = tProvider.createMetricId(DOMAIN, METRIC_TIMER_REPORT, Type.TIMER);
    }

    @Test
    public void testId_create() {
        LoggingTelemetryProvider.Metric tCount = tProvider.getMetric(tCounter);
        assertEquals("DOMAIN.METRIC_COUNTER", tCount.metricName);
        LoggingTelemetryProvider.Metric tTime = tProvider.getMetric(tTimer);
        assertEquals("DOMAIN.METRIC_TIMER", tTime.metricName);
    }

    @Test
    public void testId_get() {
        int tCount = tProvider.getMetricId(DOMAIN, METRIC_COUNTER);
        int tTime = tProvider.getMetricId(DOMAIN, METRIC_TIMER);
        assertEquals(tCounter, tCount);
        assertEquals(tTimer, tTime);
    }

    @Test
    public void testCounter_increment() {

        for (int i = 0; i < 11; i++) {
            tProvider.incrementCount(tCounter);
        }

        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tCounter);
        assertEquals(11.0, m.success);
        assertEquals(0, m.fail);
        assertEquals(0, m.startTime);
        assertEquals(0, m.totalTime);
        assertEquals("DOMAIN.METRIC_COUNTER", m.metricName);
    }

    @Test
    public void testCounter_fail() {
        for (int i = 0; i < 11; i++) {
            tProvider.fail(tCounter);
        }

        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tCounter);
        assertEquals(0.0, m.success);
        assertEquals(11, m.fail);
        assertEquals(0, m.startTime);
        assertEquals(0, m.totalTime);
        assertEquals("DOMAIN.METRIC_COUNTER", m.metricName);
    }

    @Test
    public void testTimer_start() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer);

        assertTrue(m.startTime != 0);
        assertEquals(0.0, m.success);
        assertEquals(0, m.fail);
        assertEquals(0, m.totalTime);
    }

    @Test
    @Ignore("Flaky")
    public void testTimer_stop() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer);
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        tProvider.stopTimer(tTimer);

        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);

        assertTrue(m.totalTime >= TWO_MILI);
        List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag(LOG_TAG);
        ShadowLog.LogItem lastLog = logs.get(logs.size() - 1);
        assertEquals(INFO, lastLog.type);
        assertEquals("Stopping timer: DOMAIN.METRIC_TIMER -  total:2ms", lastLog.msg);
    }

    @Test
    public void testTimer_report() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(rTimer);
        tProvider.reportTimer(rTimer, TimeUnit.NANOSECONDS, TWO_MILI);
        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);

        assertTrue(m.totalTime >= TWO_MILI);
        List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag(LOG_TAG);
        ShadowLog.LogItem lastLog = logs.get(logs.size() - 1);
        assertEquals(INFO, lastLog.type);
        assertEquals("Recording timer: DOMAIN.METRIC_TIMER_REPORT -  total:2ms", lastLog.msg);
    }

    @Test
    public void testTimer_multiStart() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        long start = System.nanoTime();
        for (int i = 0; i < 11; i++) {
            tProvider.startTimer(tTimer);
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            tProvider.stopTimer(tTimer);
        }
        long stop = System.nanoTime();

        assertEquals(11.0, m.success);
        assertEquals(0, m.fail);
        // validate totalTime, account for system call overhead by
        // clamping within timed max, and absolute known min
        assertTrue(stop - start > m.totalTime);
        assertTrue(m.totalTime > TWO_MILI * 11);
    }

    @Test
    public void testTimer_overlapStart() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);
        long start = System.nanoTime();
        tProvider.startTimer(tTimer);

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        tProvider.startTimer(tTimer); // expected does nothing
        tProvider.stopTimer(tTimer);
        // start time matches the first start call not the second
        assertTrue(m.startTime - start < 2500);
        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);
    }

    @Test
    public void testTimer_fail() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);
        tProvider.startTimer(tTimer);
        tProvider.fail(tTimer);
        assertEquals(0, m.startTime);
        assertEquals(0, m.totalTime);
        assertEquals(0.0, m.success);
        assertEquals(1, m.fail);
    }

    @Test
    public void testTimer_startFail() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer);
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        tProvider.stopTimer(tTimer);
        tProvider.startTimer(tTimer);
        tProvider.fail(tTimer);

        // start time matches the first start call not the second
        assertEquals(1.0, m.success);
        assertEquals(1, m.fail);
        assertEquals(0, m.startTime);
        assertTrue(m.totalTime >= TWO_MILI);
    }

    @Test
    public void testTimer_elapsedTimeStart() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer, TimeUnit.SECONDS, 2);

        assertTrue(m.startTime != 0);
        assertEquals(TimeUnit.SECONDS.toNanos(2), m.seedTime);
    }

    @Test
    public void testTimer_elapsedTimeStop() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer, TimeUnit.SECONDS, 2);
        tProvider.stopTimer(tTimer);

        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);

        assertTrue(m.totalTime >= TWO_SEC);
    }

    @Test
    public void testTimer_elapsedTimeStopWithEndtime() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer, TimeUnit.SECONDS, 2);
        long start = m.startTime + TWO_SEC;
        tProvider.stopTimer(tTimer, TimeUnit.NANOSECONDS, start);

        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);

        assertEquals(TWO_SEC * 2, m.totalTime);
    }

    @Test
    public void testTimer_StopWithEndtime() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer);
        long start = m.startTime + TWO_SEC;
        tProvider.stopTimer(tTimer, TimeUnit.NANOSECONDS, start);

        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);

        assertEquals(TWO_SEC, m.totalTime);
    }

    @Test
    public void testTimer_StopWithEndtimeInSeconds() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        tProvider.startTimer(tTimer);
        long startNanos = m.startTime;
        long startMillis = TimeUnit.NANOSECONDS.toMillis(startNanos);
        long endMillis = TimeUnit.SECONDS.toMillis(2) + startMillis;
        tProvider.stopTimer(tTimer, TimeUnit.MILLISECONDS, endMillis);

        assertEquals(1.0, m.success);
        assertEquals(0, m.fail);

        assertEquals(TimeUnit.MILLISECONDS.toNanos(endMillis) - startNanos, m.totalTime);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTimer_StartWithInvalidMetricId() throws AssertionError {
        tProvider.startTimer(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTimer_StopWithInvalidMetricId() throws AssertionError {
        tProvider.stopTimer(-1);
    }

    @Test
    public void testGetPerformanceMetrics_WithFail() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);
        tProvider.startTimer(tTimer);
        long startNanos = m.startTime;
        long startMillis = TimeUnit.NANOSECONDS.toMillis(startNanos);
        long endMillis = TimeUnit.SECONDS.toMillis(2) + startMillis;
        tProvider.stopTimer(tTimer, TimeUnit.MILLISECONDS, endMillis);
        tProvider.incrementCount(tCounter, 5);

        List<MetricInfo> metricInfoList = tProvider.getPerformanceMetrics();
        assertEquals(3, metricInfoList.size());
        for (MetricInfo metricInfo : metricInfoList) {
            if (tProvider.getMetric(tTimer).metricName.equals(metricInfo.getName())) {
                assertEquals(2000, metricInfo.getValue(), 1.0f);
            } else if (tProvider.getMetric(tCounter).metricName.equals(metricInfo.getName())) {
                assertEquals(5, metricInfo.getValue(), 0.01f);
            }
        }
    }

    @Test
    public void testIncrementCounterByDoubleValue() {
        LoggingTelemetryProvider.Metric tCount = tProvider.getMetric(tCounter);
        tProvider.incrementCount(tCounter, 5.0);

        assertEquals(5.0, tCount.success);
    }
}
