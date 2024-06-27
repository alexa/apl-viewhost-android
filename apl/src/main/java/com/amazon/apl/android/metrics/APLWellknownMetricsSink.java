/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation for collecting metrics
 */
public abstract class APLWellknownMetricsSink implements IMetricsSink {
    public static final String WELLKNOWNMETRIC_RENDER_DOCUMENT = "APL.renderDocument";
    public static final String WELLKNOWNMETRIC_CONTENT_CREATE = "APL.Content.create";
    public static final String WELLKNOWNMETRIC_EXTENSIONS_REGISTER = "APL.Extensions.register";
    public static final String WELLKNOWNMETRIC_ROOT_CONTEXT_INFLATE = "APL.RootContext.inflate";
    public static final String WELLKNOWNMETRIC_VIEWS_INFLATE = "APL.Views.inflate";
    public static final String WELLKNOWNMETRIC_VIEWS_LAYOUT = "APL.Views.layout";
    public static final String WELLKNOWNMETRIC_TEXT_MEASURE = "APL.Text.measure";
    public static final String WELLKNOWNMETRIC_PREPARE_DOCUMENT = "APL.prepareDocument";

    private final Map<String, String> milestoneToWellKnownMap;
    private final Map<String, List<Long>> wellKnownToDurationMap;
    private final Map<String, Map<String, String>> metadata;
    private final Map<String, Integer> counters;
    private final Map<Long, MetricsEvent> mSegmentStarts;
    private final Map<Long, MetricsEvent> mSegmentEnds;

    public APLWellknownMetricsSink(long seed) {
        milestoneToWellKnownMap = new HashMap<>();
        wellKnownToDurationMap = new HashMap<>();
        counters = new HashMap<>();
        metadata = new HashMap<>();
        mSegmentStarts = new HashMap<>();
        mSegmentEnds= new HashMap<>();

        initKPIsWithSeed(seed);
    }

    private void initKPIsWithSeed(long seed) {
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.EXTENSION_REGISTRATION_START_MILESTONE, WELLKNOWNMETRIC_EXTENSIONS_REGISTER);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.EXTENSION_REGISTRATION_END_MILESTONE, WELLKNOWNMETRIC_EXTENSIONS_REGISTER);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.EXTENSION_REGISTRATION_FAILED_MILESTONE, WELLKNOWNMETRIC_EXTENSIONS_REGISTER);

        milestoneToWellKnownMap.put(MetricsMilestoneConstants.CONTENT_CREATE_START_MILESTONE, WELLKNOWNMETRIC_CONTENT_CREATE);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.CONTENT_CREATE_END_MILESTONE, WELLKNOWNMETRIC_CONTENT_CREATE);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.CONTENT_CREATE_FAILED_MILESTONE, WELLKNOWNMETRIC_CONTENT_CREATE);

        milestoneToWellKnownMap.put(MetricsMilestoneConstants.ROOTCONTEXT_INFLATE_START_MILESTONE, WELLKNOWNMETRIC_ROOT_CONTEXT_INFLATE);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.ROOTCONTEXT_INFLATE_END_MILESTONE, WELLKNOWNMETRIC_ROOT_CONTEXT_INFLATE);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.ROOTCONTEXT_INFLATE_FAILED_MILESTONE, WELLKNOWNMETRIC_ROOT_CONTEXT_INFLATE);

        milestoneToWellKnownMap.put(MetricsMilestoneConstants.APLLAYOUT_VIEW_INFLATE_START_MILESTONE, WELLKNOWNMETRIC_VIEWS_INFLATE);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.APLLAYOUT_VIEW_INFLATE_END_MILESTONE, WELLKNOWNMETRIC_VIEWS_INFLATE);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.APLLAYOUT_VIEW_INFLATE_FAILED_MILESTONE, WELLKNOWNMETRIC_VIEWS_INFLATE);

        milestoneToWellKnownMap.put(MetricsMilestoneConstants.APLLAYOUT_LAYOUT_START_MILESTONE, WELLKNOWNMETRIC_VIEWS_LAYOUT);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.APLLAYOUT_LAYOUT_END_MILESTONE, WELLKNOWNMETRIC_VIEWS_LAYOUT);

        milestoneToWellKnownMap.put(MetricsMilestoneConstants.RENDER_END_MILESTONE, WELLKNOWNMETRIC_RENDER_DOCUMENT);
        milestoneToWellKnownMap.put(MetricsMilestoneConstants.RENDER_FAILED_MILESTONE, WELLKNOWNMETRIC_RENDER_DOCUMENT);

        initWellKnownDuration();

        // Apply the seed.
        wellKnownToDurationMap.get(WELLKNOWNMETRIC_RENDER_DOCUMENT).add(seed);
        wellKnownToDurationMap.get(WELLKNOWNMETRIC_PREPARE_DOCUMENT).add(seed);
    }

    private void initWellKnownDuration() {
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_RENDER_DOCUMENT, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_PREPARE_DOCUMENT, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_CONTENT_CREATE, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_EXTENSIONS_REGISTER, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_ROOT_CONTEXT_INFLATE, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_VIEWS_INFLATE, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_VIEWS_LAYOUT, new ArrayList<>());
        wellKnownToDurationMap.put(WELLKNOWNMETRIC_TEXT_MEASURE, new ArrayList<>());
    }

    @Override
    public void onDocumentFinish() {
        logAndReset();
    }

    public void logAndReset() {
        for (Map.Entry<String, List<Long>> entry : wellKnownToDurationMap.entrySet()) {
            String kpi = entry.getKey();
            List<Long> durations = entry.getValue();

            if (durations.size() == 2) {
                Long duration = durations.get(1) - durations.get(0);
                Map<String, String> metadataForKpi = metadata.get(kpi);
                timerPublished(kpi, duration, metadataForKpi);
            }
        }

        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            String counterName = entry.getKey();
            int counterValue = entry.getValue();
            Map<String, String> metadataForCounter = metadata.get(counterName);
            counterPublished(counterName, counterValue, metadataForCounter);
        }

        for (long relateID: mSegmentEnds.keySet()) {
            MetricsEvent end = mSegmentEnds.get(relateID);
            MetricsEvent start = mSegmentStarts.get(relateID);
            if (start != null) {
                long duration = end.getValue() - start.getValue();
                segmentPublished(end.getMetricName(), duration, end.getMetaData());
            }
        }
        clear();
    }

    private void clear() {
        wellKnownToDurationMap.clear();;
        counters.clear();
        mSegmentStarts.clear();
        mSegmentEnds.clear();

        initWellKnownDuration();
    }

    public void updateKPI(MetricsEvent event) {
        String wellknown = milestoneToWellKnownMap.get(event.getMetricName());

        if (wellknown != null) {
            wellKnownToDurationMap.get(wellknown).add(event.getValue());
            updateMetadata(wellknown, event.getMetaData());
        }
    }

    public void aggregateCounter(MetricsEvent event) {
        String counterName = event.getMetricName();
        int counterValue = counters.get(counterName) == null ? 0 : counters.get(counterName);
        counters.put(counterName, counterValue + (int) event.getValue());
        updateMetadata(counterName, event.getMetaData());
    }

    private void updateMetadata(String name, Map<String, String> newMetadata) {
        if (newMetadata != null && !newMetadata.isEmpty()) {
            Map<String, String> metadata = this.metadata.get(name);
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.putAll(newMetadata);
            this.metadata.put(name, metadata);
        }
    }

    @Override
    public void metricPublished(MetricsEvent event) {
        if (MetricEventType.MILESTONE.equals(event.getEventType())) {
            updateKPI(event);
        } else if (MetricEventType.COUNTER.equals(event.getEventType())) {
            aggregateCounter(event);
        } else {
            //collect segment metrics
            addSegmentMetric(event);
        }
    }

    private void addSegmentMetric(MetricsEvent event) {
        if (MetricEventType.SEGMENT_START.equals(event.getEventType())) {
            mSegmentStarts.put(event.getMetricId(), event);
        } else {
            mSegmentEnds.put(event.getRelateId(), event);
        }
    }

    public abstract void timerPublished(String name, long duration, Map<String, String> metadata);

    public abstract void counterPublished(String name, long value, Map<String, String> metadata);

    public abstract void segmentPublished(String name, long duration, Map<String, String> metadata);
}
