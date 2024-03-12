/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.devtools.models;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.apl.devtools.models.log.LogEntry;
import com.amazon.apl.devtools.models.log.LogEntryAddedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class ViewTypeTargetTest {
    @Mock
    private Session mockSession;
    private ViewTypeTarget viewTypeTarget;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockSession.isLogEnabled()).thenReturn(true);
        viewTypeTarget = new ViewTypeTarget("mockName");
        viewTypeTarget.registerSession(mockSession);
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
}

