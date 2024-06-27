/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.performance;

import android.os.Handler;
import com.amazon.apl.android.metrics.MetricEventType;
import com.amazon.apl.android.metrics.MetricsEvent;
import com.amazon.apl.android.utils.MetricInfo;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.amazon.apl.android.metrics.MetricsMilestoneConstants.RENDER_END_MILESTONE;
import static com.amazon.apl.android.metrics.MetricsMilestoneConstants.RENDER_START_MILESTONE;
import static com.amazon.apl.android.metrics.MetricsMilestoneConstants.ROOTCONTEXT_INFLATE_END_MILESTONE;
import static com.amazon.apl.android.metrics.MetricsMilestoneConstants.ROOTCONTEXT_INFLATE_START_MILESTONE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class DTMetricsSinkTest {
    @Mock
    private MetricsEvent mockMetricsEvent;
    @Mock
    private MetricsEvent mockStartMetricsEvent;
    @Mock
    private MetricsEvent mockEndMetricsEvent;
    @Mock
    private Handler mockHandler;

    private ArgumentCaptor<Runnable> mHandlerArgumentCaptor;
    private DTMetricsSink mDTMetricsSink;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mHandlerArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        mDTMetricsSink = new DTMetricsSink(mockHandler);
    }

    @Test
    public void testCounterWhenMetricEventIsEmittedOnce() {
        when(mockMetricsEvent.getEventType()).thenReturn(MetricEventType.COUNTER);
        when(mockMetricsEvent.getMetricName()).thenReturn("counterMetric");
        when(mockMetricsEvent.getValue()).thenReturn(100L);

        mDTMetricsSink.metricPublished(mockMetricsEvent);

        verify(mockHandler).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        final List<MetricInfo> recordedMetric = mDTMetricsSink.retrieveMetrics();
        assertEquals(1, recordedMetric.size());
        final MetricInfo counterMetric = recordedMetric.get(0);
        assertEquals(mockMetricsEvent.getMetricName(), counterMetric.getName());
        assertEquals(mockMetricsEvent.getValue(), counterMetric.getValue(), 0);
    }

    @Test
    public void testCounterWhenMetricEventIsEmittedTwice() {
        // Setup
        when(mockMetricsEvent.getEventType()).thenReturn(MetricEventType.COUNTER);
        when(mockMetricsEvent.getMetricName()).thenReturn("counterMetric");
        when(mockMetricsEvent.getValue()).thenReturn(100L);

        // run
        mDTMetricsSink.metricPublished(mockMetricsEvent);
        verify(mockHandler).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        mDTMetricsSink.metricPublished(mockMetricsEvent);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        // Verify
        final List<MetricInfo> recordedMetric = mDTMetricsSink.retrieveMetrics();
        assertEquals(1, recordedMetric.size());
        final MetricInfo counterMetric = recordedMetric.get(0);
        assertEquals(mockMetricsEvent.getMetricName(), counterMetric.getName());
        assertEquals(mockMetricsEvent.getValue() * 2, counterMetric.getValue(), 0);
    }

    @Test
    public void testMileStoneVerifyTheCorrectMetricGetsCalculated() {
        // Start milestone.
        when(mockStartMetricsEvent.getEventType()).thenReturn(MetricEventType.MILESTONE);
        when(mockStartMetricsEvent.getMetricName()).thenReturn(ROOTCONTEXT_INFLATE_START_MILESTONE);
        when(mockStartMetricsEvent.getValue()).thenReturn(100L);

        // Record Event
        mDTMetricsSink.metricPublished(mockStartMetricsEvent);
        verify(mockHandler).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        // End MileStone
        when(mockEndMetricsEvent.getEventType()).thenReturn(MetricEventType.MILESTONE);
        when(mockEndMetricsEvent.getMetricName()).thenReturn(ROOTCONTEXT_INFLATE_END_MILESTONE);
        when(mockEndMetricsEvent.getValue()).thenReturn(1000L);

        // Record Event
        mDTMetricsSink.metricPublished(mockEndMetricsEvent);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        // Verify
        final List<MetricInfo> recordedMetric = mDTMetricsSink.retrieveMetrics();
        assertEquals(1, recordedMetric.size());
        final MetricInfo renderDocument = recordedMetric.get(0);
        final double expectedLatency = (double) (1000L - 100L) / 1_000_000_000;

        assertEquals(expectedLatency, renderDocument.getValue(), 0);
        assertEquals("APL.RootContext.inflate", renderDocument.getName());
    }

    @Test
    public void testSegmentMetric() {
        final String segmentName = "segmentEvent";
        // Start Segment.
        when(mockStartMetricsEvent.getEventType()).thenReturn(MetricEventType.SEGMENT_START);
        when(mockStartMetricsEvent.getMetricName()).thenReturn(segmentName);
        when(mockStartMetricsEvent.getValue()).thenReturn(100L);
        when(mockStartMetricsEvent.getMetricId()).thenReturn(1L);

        // Record Event
        mDTMetricsSink.metricPublished(mockStartMetricsEvent);
        verify(mockHandler).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        // End Segment
        when(mockEndMetricsEvent.getEventType()).thenReturn(MetricEventType.SEGMENT_END);
        when(mockEndMetricsEvent.getMetricName()).thenReturn(segmentName);
        when(mockEndMetricsEvent.getValue()).thenReturn(1000L);
        when(mockEndMetricsEvent.getRelateId()).thenReturn(1L);

        // Record Event
        mDTMetricsSink.metricPublished(mockEndMetricsEvent);
        verify(mockHandler, times(2)).post(mHandlerArgumentCaptor.capture());
        mHandlerArgumentCaptor.getValue().run();

        // Verify
        final List<MetricInfo> recordedMetric = mDTMetricsSink.retrieveMetrics();
        assertEquals(1, recordedMetric.size());
        final MetricInfo segmentMetric = recordedMetric.get(0);
        final double expectedLatency = (double) (1000L - 100L) / 1_000_000_000;

        assertEquals(expectedLatency, segmentMetric.getValue(), 0);
        assertEquals(segmentName, segmentMetric.getName());
    }
}
