package com.amazon.apl.android.metrics.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.ITimestampGenerator;
import com.amazon.apl.android.metrics.MetricEventType;
import com.amazon.apl.android.metrics.MetricsEvent;
import com.amazon.apl.viewhost.utils.TestHandler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class MetricsRecorderTest extends AbstractDocUnitTest{
    IMetricsRecorder mMetricsRecorder;
    MetricsSink mMetricsSink;

    ITimestampGenerator mTimestampGenerator;

    TestHandler mHandler;
    @Before
    public void setup() {
        mTimestampGenerator = new MockTimestampgenerator();
        mHandler = new TestHandler();
        mMetricsRecorder = new MetricsRecorder(mTimestampGenerator, mHandler);
        mMetricsSink = new MetricsSink();
        mMetricsRecorder.addSink(mMetricsSink);
    }

    @Test
    public void testMilestone() throws InterruptedException {
        mMetricsRecorder.recordMilestone("M1");
        assertFalse(mHandler.isEmpty());
        mHandler.flush();
        MetricsEvent event = mMetricsSink.metricsEventList.get(0);
        Assert.assertEquals(MetricEventType.MILESTONE, event.getEventType());
        assertNotNull(event.getMetricId());
        assertEquals("M1", event.getMetricName());
        assertEquals(10L, event.getValue());
        assertEquals(0, event.getMetaData().size());
    }

    @Test
    public void testMultipleMilestones() throws InterruptedException {
        mMetricsRecorder.recordMilestone("Milestone1");
        mMetricsRecorder.recordMilestone("Milestone2");
        assertFalse(mHandler.isEmpty());
        mHandler.flush();
        MetricsEvent event = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.MILESTONE, event.getEventType());
        assertNotNull(event.getMetricId());
        assertEquals("Milestone1", event.getMetricName());
        assertEquals(10L, event.getValue());
        assertEquals(0, event.getMetaData().size());

        MetricsEvent event2 = mMetricsSink.metricsEventList.get(1);
        assertEquals(MetricEventType.MILESTONE, event2.getEventType());
        assertNotNull(event2.getMetricId());
        assertEquals("Milestone2", event2.getMetricName());
        assertEquals(20L, event2.getValue());
        assertEquals(0, event2.getMetaData().size());
    }

    @Test
    public void testMilestoneWithRecorderMetadata() throws InterruptedException {
        mMetricsRecorder.mergeMetadata("foo", "bar");
        mMetricsRecorder.recordMilestone("M1");

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent event = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.MILESTONE, event.getEventType());
        assertNotNull(event.getMetricId());
        assertEquals("M1", event.getMetricName());
        assertEquals(10L, event.getValue());
        assertEquals("bar", event.getMetaData().get("foo"));

    }

    @Test
    public void testSegments() throws InterruptedException {
        ITimer timer = mMetricsRecorder.startTimer("Timer1", null);
        timer.stop();

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent segmentStartMetric = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.SEGMENT_START, segmentStartMetric.getEventType());
        assertNotNull(segmentStartMetric.getMetricId());
        assertEquals("Timer1", segmentStartMetric.getMetricName());
        assertEquals(10L, segmentStartMetric.getValue());
        assertEquals(0, segmentStartMetric.getMetaData().size());
        assertEquals(0L, segmentStartMetric.getRelateId());

        MetricsEvent segmentEndMetric = mMetricsSink.metricsEventList.get(1);
        assertEquals(MetricEventType.SEGMENT_END, segmentEndMetric.getEventType());
        assertNotNull(segmentEndMetric.getMetricId());
        assertEquals("Timer1", segmentEndMetric.getMetricName());
        assertEquals(20L, segmentEndMetric.getValue());
        assertEquals(segmentStartMetric.getMetricId(), segmentEndMetric.getRelateId());

    }

    @Test
    public void testFailedSegment() throws InterruptedException {
        ITimer timer = mMetricsRecorder.startTimer("Timer1", null);
        timer.fail();

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent segmentStartMetric = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.SEGMENT_START, segmentStartMetric.getEventType());
        assertNotNull(segmentStartMetric.getMetricId());
        assertEquals("Timer1", segmentStartMetric.getMetricName());
        assertEquals(10L, segmentStartMetric.getValue());
        assertEquals(0, segmentStartMetric.getMetaData().size());
        assertEquals(0L, segmentStartMetric.getRelateId());

        MetricsEvent segmentEndMetric = mMetricsSink.metricsEventList.get(1);
        assertEquals(MetricEventType.SEGMENT_FAILED, segmentEndMetric.getEventType());
        assertNotNull(segmentEndMetric.getMetricId());
        assertEquals("Timer1", segmentEndMetric.getMetricName());
        assertEquals(20L, segmentEndMetric.getValue());
        assertEquals(segmentStartMetric.getMetricId(), segmentEndMetric.getRelateId());
    }

    @Test
    public void testSegmentsWithRecorderMetadata() throws InterruptedException {
        mMetricsRecorder.mergeMetadata("globalMD", "globalVal");
        ITimer timer = mMetricsRecorder.startTimer("Timer1", null);
        timer.stop();

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent segmentStartMetric = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.SEGMENT_START, segmentStartMetric.getEventType());
        assertNotNull(segmentStartMetric.getMetricId());
        assertEquals("Timer1", segmentStartMetric.getMetricName());
        assertEquals(10L, segmentStartMetric.getValue());
        assertEquals("globalVal", segmentStartMetric.getMetaData().get("globalMD"));
        assertEquals(1, segmentStartMetric.getMetaData().size());
        assertEquals(0L, segmentStartMetric.getRelateId());

        MetricsEvent segmentEndMetric = mMetricsSink.metricsEventList.get(1);
        assertEquals(MetricEventType.SEGMENT_END, segmentEndMetric.getEventType());
        assertNotNull(segmentEndMetric.getMetricId());
        assertEquals("Timer1", segmentEndMetric.getMetricName());
        assertEquals(20L, segmentEndMetric.getValue());
        assertEquals(segmentStartMetric.getMetricId(), segmentEndMetric.getRelateId());
    }

    @Test
    public void testSegmentsWithSegmentMetadata() throws InterruptedException {
        mMetricsRecorder.mergeMetadata("foo", "bar");
        Map<String, String> timerMetaData = new HashMap<>();
        timerMetaData.put("foo", "barTimer");
        timerMetaData.put("key", "val");
        ITimer timer = mMetricsRecorder.startTimer("Timer1", timerMetaData);
        timer.stop();

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent segmentStartMetric = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.SEGMENT_START, segmentStartMetric.getEventType());
        assertNotNull(segmentStartMetric.getMetricId());
        assertEquals("Timer1", segmentStartMetric.getMetricName());
        assertEquals(10L, segmentStartMetric.getValue());
        assertEquals("barTimer", segmentStartMetric.getMetaData().get("foo"));
        assertEquals("val", segmentStartMetric.getMetaData().get("key"));
        assertEquals(2, segmentStartMetric.getMetaData().size());
        assertEquals(0L, segmentStartMetric.getRelateId());

        MetricsEvent segmentEndMetric = mMetricsSink.metricsEventList.get(1);
        assertEquals(MetricEventType.SEGMENT_END, segmentEndMetric.getEventType());
        assertNotNull(segmentEndMetric.getMetricId());
        assertEquals("Timer1", segmentEndMetric.getMetricName());
        assertEquals(20L, segmentEndMetric.getValue());
        assertEquals("barTimer", segmentStartMetric.getMetaData().get("foo"));
        assertEquals("val", segmentStartMetric.getMetaData().get("key"));
        assertEquals(segmentStartMetric.getMetricId(), segmentEndMetric.getRelateId());
    }
    @Test
    public void testCounterIncremented() throws InterruptedException {
        ICounter counter = mMetricsRecorder.createCounter("counter1");
        counter.increment(1); //1
        counter.increment(5); //6
        counter.increment(-2); //4

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent event = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.COUNTER, event.getEventType());
        assertNotNull(event.getMetricId());
        assertEquals("counter1", event.getMetricName());
        assertEquals(1L, event.getValue());
        assertEquals(0, event.getMetaData().size());

        MetricsEvent event2 = mMetricsSink.metricsEventList.get(1);
        assertEquals(MetricEventType.COUNTER, event2.getEventType());
        assertNotNull(event2.getMetricId());
        assertEquals("counter1", event2.getMetricName());
        assertEquals(5L, event2.getValue());
        assertEquals(0, event2.getMetaData().size());

        MetricsEvent event3 = mMetricsSink.metricsEventList.get(2);
        assertEquals(MetricEventType.COUNTER, event3.getEventType());
        assertNotNull(event3.getMetricId());
        assertEquals("counter1", event3.getMetricName());
        assertEquals(-2L, event3.getValue());
        assertEquals(0, event3.getMetaData().size());
    }

    @Test
    public void testCounterWithRecorderMetadata() throws InterruptedException {
        mMetricsRecorder.mergeMetadata("foo", "bar");
        ICounter counter = mMetricsRecorder.createCounter("counter1");
        counter.increment(1); //1

        assertFalse(mHandler.isEmpty());
        mHandler.flush();

        MetricsEvent event = mMetricsSink.metricsEventList.get(0);
        assertEquals(MetricEventType.COUNTER, event.getEventType());
        assertNotNull(event.getMetricId());
        assertEquals("counter1", event.getMetricName());
        assertEquals(1L, event.getValue());
        assertEquals("bar", event.getMetaData().get("foo"));
    }

    class MetricsSink implements IMetricsSink {
        public List<MetricsEvent> metricsEventList;
        MetricsSink() {
            metricsEventList = new ArrayList<>();
        }
        @Override
        public void metricPublished(MetricsEvent event) {
            metricsEventList.add(event);
        }
    }
    class MockTimestampgenerator implements ITimestampGenerator {
        long timestamp = 0;
        @Override
        public long generateTimeStamp() {
            timestamp = timestamp + 10;
            return timestamp;
        }
    }

}
