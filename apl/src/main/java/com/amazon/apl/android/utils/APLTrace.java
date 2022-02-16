/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.os.Trace;

import java.util.EnumMap;
import java.util.Map;

/**
 * Utility class for starting and stopping Trace points. Map is needed for avoiding repeated String allocations
 * for concatenating the Agent name.
 */
public class APLTrace {
    private static final char AGENT_CLASS_SEPARATOR = '-';
    private static final char CLASS_METHOD_SEPARATOR = '.';

    private final Map<TracePoint, String> mTracePointSectionNameMap = new EnumMap<>(TracePoint.class);
    private String mAgentName;

    /**
     * Initialize a set of TracePoints for a given AgentName
     * @param agentName the agent name
     */
    public APLTrace(final String agentName) {
        setAgentName(agentName);
    }

    public void setAgentName(String agentName) {
        if (!agentName.equals(mAgentName)) {
            for (TracePoint tracePoint : TracePoint.values()) {
                mTracePointSectionNameMap.put(tracePoint, createSectionName(tracePoint));
            }
        }
        mAgentName = agentName;
    }

    public String getAgentName() {
        return mAgentName;
    }

    private String createSectionName(TracePoint tracePoint) {
        return new StringBuilder()
                .append(mAgentName)
                .append(AGENT_CLASS_SEPARATOR)
                .append(tracePoint.getClassName())
                .append(CLASS_METHOD_SEPARATOR)
                .append(tracePoint.getMethodName())
                .toString();
    }

    /**
     * Starts a Trace for a TracePoint.
     * @param tracePoint the tracepoint to start
     */
    public void startTrace(TracePoint tracePoint) {
        Trace.beginSection(mTracePointSectionNameMap.get(tracePoint));
    }

    /**
     * Ends a trace.
     */
    public void endTrace() {
        Trace.endSection();
    }

    /**
     * Starts an auto closing trace.
     * @param tracePoint the tracepoint to start
     * @return  an auto-closable trace.
     */
    public AutoTrace startAutoTrace(TracePoint tracePoint) {
        return new AutoTrace(tracePoint);
    }

    /**
     * Trace utility that implements {@link AutoCloseable} for ensuring that the trace is closed.
     *
     * Use with try-with-resources block:
     *
     * try (APLTrace.AutoTrace trace = mAplTrace.startAutoTrace(TracePoint) {
     *     // code to trace
     * }
     *
     * Note: Do not use this API in a tight loop (i.e. doFrame) to avoid creating unnecessary temporary
     * objects for GC. Prefer the api {@link APLTrace#startTrace(TracePoint)}.
     */
    public class AutoTrace implements AutoCloseable {
        AutoTrace(TracePoint tracePoint) {
            startTrace(tracePoint);
        }

        @Override
        public void close() {
            Trace.endSection();
        }
    }
}
