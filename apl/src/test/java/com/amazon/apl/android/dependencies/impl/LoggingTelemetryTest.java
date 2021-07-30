/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies.impl;

import com.amazon.apl.android.providers.ITelemetryProvider.Type;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class LoggingTelemetryTest {


    private final static String DOMAIN = "DOMAIN";
    private final static String METRIC_COUNTER = "METRIC_COUNTER";
    private final static String METRIC_TIMER = "METRIC_TIMER";
    private final static long TWO_MILI = TimeUnit.MILLISECONDS.toNanos(2);
    private final static long TWO_SEC = TimeUnit.SECONDS.toNanos(2);

    private LoggingTelemetryProvider tProvider;
    private int tCounter;
    private int tTimer;


    @Before
    public void before() {
        tProvider = spy(new LoggingTelemetryProvider());
        doAnswer(invocation -> System.nanoTime()).when(tProvider).realtimeNanos();
        tCounter = tProvider.createMetricId(DOMAIN, METRIC_COUNTER, Type.COUNTER);
        tTimer = tProvider.createMetricId(DOMAIN, METRIC_TIMER, Type.TIMER);
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
        assertEquals(11, m.success);
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
        assertEquals(0, m.success);
        assertEquals(11, m.fail);
        assertEquals(0, m.startTime);
        assertEquals(0, m.totalTime);
        assertEquals("DOMAIN.METRIC_COUNTER", m.metricName);

    }


    @Test
    public void testTimer_start() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        long start = System.nanoTime();
        tProvider.startTimer(tTimer);
        long next = System.nanoTime();

        // validate start time, account for system call overhead by
        // clamping within timestamp
        assertTrue(m.startTime > start);
        assertTrue(m.startTime < next);

        assertEquals(0, m.success);
        assertEquals(0, m.fail);
        assertEquals(0, m.totalTime);
    }

    @Test
    public void testTimer_stop() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        long start = System.nanoTime();
        tProvider.startTimer(tTimer);
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        tProvider.stopTimer(tTimer);
        long stop = System.nanoTime();

        assertEquals(1, m.success);
        assertEquals(0, m.fail);

        // validate totalTime, account for system call overhead by
        // clamping within timed max, and absolute known min
        assertTrue(stop - start > m.totalTime);
        assertTrue(m.totalTime > TWO_MILI);
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

        assertEquals(11, m.success);
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
        assertEquals(1, m.success);
        assertEquals(0, m.fail);
    }


    @Test
    public void testTimer_fail() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);
        tProvider.startTimer(tTimer);
        tProvider.fail(tTimer);
        assertEquals(0, m.startTime);
        assertEquals(0, m.totalTime);
        assertEquals(0, m.success);
        assertEquals(1, m.fail);
    }

    @Test
    public void testTimer_startFail() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        long start = System.nanoTime();
        tProvider.startTimer(tTimer);
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        tProvider.stopTimer(tTimer);
        long stop = System.nanoTime();
        tProvider.startTimer(tTimer);
        tProvider.fail(tTimer);

        // start time matches the first start call not the second
        assertEquals(1, m.success);
        assertEquals(1, m.fail);
        assertEquals(0, m.startTime);
        // validate totalTime, account for system call overhead by
        // clamping within timed max, and absolute known min
        assertTrue(stop - start > m.totalTime);
        assertTrue(m.totalTime > TWO_MILI);

    }

    @Test
    public void testTimer_elapsedTimeStart() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        long start = System.nanoTime();
        tProvider.startTimer(tTimer, TimeUnit.SECONDS, 2);
        long next = System.nanoTime();

        // validate start time, account for system call overhead by
        // clamping within timestamp
        assertTrue(m.startTime > start - TWO_SEC);
        assertTrue(m.startTime < next - TWO_SEC);
    }

    @Test
    public void testTimer_elapsedTimeStop() {
        LoggingTelemetryProvider.Metric m = tProvider.getMetric(tTimer);

        long start = System.nanoTime();
        tProvider.startTimer(tTimer, TimeUnit.SECONDS, 2);
        tProvider.stopTimer(tTimer);
        long stop = System.nanoTime();

        assertEquals(1, m.success);
        assertEquals(0, m.fail);

        // validate totalTime, account for system call overhead by
        // clamping within timed max, and absolute known min
        assertTrue(stop - start + TWO_SEC > m.totalTime);
        assertTrue(m.totalTime > TWO_SEC);
    }

}
