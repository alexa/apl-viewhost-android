/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.devtools.models.log;

import com.amazon.apl.android.Session;

public class LogEntry {
    private final Session.LogEntryLevel level;
    private final Session.LogEntrySource source;
    private final String text;
    private final double timestamp;
    private final Object[] arguments;

    public LogEntry(Session.LogEntryLevel level, Session.LogEntrySource source, String text, double timestamp, Object[] arguments) {
        this.level = level;
        this.source = source;
        this.text = text;
        this.timestamp = timestamp;
        this.arguments = arguments;
    }

    public Session.LogEntryLevel getLevel() {
        return level;
    }

    public Session.LogEntrySource getSource() {
        return source;
    }

    public String getText() {
        return text;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public Object[] getArguments() {
        return arguments;
    }
}

