package com.amazon.apl.devtools.models.log;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.apl.android.Session;
import com.amazon.apl.devtools.enums.EventMethod;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LogEntryAddedEventUnitTest {

    @Test
    public void testLogEntryAddedEvent() {
        String sessionId = "123";
        Session.LogEntryLevel level = Session.LogEntryLevel.INFO;
        String messageText = "Test message";
        Session.LogEntrySource source = Session.LogEntrySource.VIEW;
        double timestamp = 123456789.0;
        Object[] arguments = new Object[]{1};

        LogEntryAddedEvent logEntryAddedEvent = new LogEntryAddedEvent(sessionId, level, source, messageText, timestamp, arguments);

        assertEquals(EventMethod.LOG_ENTRY_ADDED, logEntryAddedEvent.getMethod());
        assertEquals(sessionId, logEntryAddedEvent.getSessionId());

        LogEntryAddedEvent.Params params = logEntryAddedEvent.getParams();
        assertEquals(level, params.getLevel());
        assertEquals(messageText, params.getMessageText());
        assertEquals(source, params.getSource());
        assertEquals(timestamp, params.getTimestamp(), 0.0);

        Object[] actualArguments = params.getArguments();
        assertEquals(1, actualArguments.length);
        assertEquals(1, actualArguments[0]);
    }

    @Test
    public void testLogEntryAddedEventToJSONObject() throws Exception {
        String sessionId = "123";
        Session.LogEntryLevel level = Session.LogEntryLevel.INFO;
        String messageText = "Test message";
        Session.LogEntrySource source = Session.LogEntrySource.VIEW;
        double timestamp = 123456789.0;
        Object[] arguments = new Object[]{1};

        LogEntryAddedEvent logEntryAddedEvent = new LogEntryAddedEvent(sessionId, level, source, messageText, timestamp, arguments);

        JSONObject superJsonObject = mock(JSONObject.class);
        when(superJsonObject.put(anyString(), any())).thenReturn(superJsonObject);
        JSONObject jsonObject = logEntryAddedEvent.toJSONObject();

        assertEquals(EventMethod.LOG_ENTRY_ADDED.toString(), jsonObject.getString("method"));
        assertEquals(sessionId, jsonObject.getString("sessionId"));

        JSONObject paramsJson = jsonObject.getJSONObject("params");
        assertEquals(level.toString().toLowerCase(), paramsJson.getJSONObject("entry").getString("level"));
        assertEquals(messageText, paramsJson.getJSONObject("entry").getString("text"));
        assertEquals(source.toString().toLowerCase(), paramsJson.getJSONObject("entry").getString("source"));
        assertEquals(timestamp, paramsJson.getJSONObject("entry").getDouble("timestamp"), 0.0);

        JSONArray argumentsArray = paramsJson.getJSONObject("entry").getJSONArray("arguments");
        List<Object> actualArguments = new ArrayList<>();
        for (int i = 0; i < argumentsArray.length(); i++) {
            actualArguments.add(argumentsArray.get(i));
        }

        assertEquals(arguments.length, actualArguments.size());
        for (int i = 0; i < arguments.length; i++) {
            assertEquals(arguments[i], actualArguments.get(i));
        }
    }
}
