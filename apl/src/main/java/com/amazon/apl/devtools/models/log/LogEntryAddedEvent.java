/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.log;

import android.util.Log;

import com.amazon.apl.android.Session;
import com.amazon.apl.devtools.enums.EventMethod;
import com.amazon.apl.devtools.models.common.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LogEntryAddedEvent extends Event {
    private static final String TAG = LogEntryAddedEvent.class.getSimpleName();
    private final LogEntryAddedEvent.Params mParams;

    public LogEntryAddedEvent(String mSessionId, Session.LogEntryLevel level, Session.LogEntrySource source, String messageText, double timestamp, Object[] arguments) {
        super(EventMethod.LOG_ENTRY_ADDED, mSessionId);
        mParams = new LogEntryAddedEvent.Params(level, messageText, source, timestamp, arguments);
    }

    public LogEntryAddedEvent.Params getParams() {
        return mParams;
    }

    public static class Params {
        private final Session.LogEntryLevel mLevel;
        private final String mMessageText;
        private final Session.LogEntrySource mSource;
        private final double mTimestamp;
        private final Object[] mArguments;

        public Params(Session.LogEntryLevel level, String messageText, Session.LogEntrySource source, double timestamp, Object[] arguments) {
            mLevel = level;
            mMessageText = messageText;
            mSource = source;
            mTimestamp = timestamp;
            mArguments = arguments;
        }

        public Session.LogEntryLevel getLevel() {
            return mLevel;
        }

        public String getMessageText() {
            return mMessageText;
        }

        public Session.LogEntrySource getSource() {
            return mSource;
        }

        public double getTimestamp() {
            return mTimestamp;
        }

        public Object[] getArguments() {
            return mArguments;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.LOG_ENTRY_ADDED + " event object");
        JSONObject jsonParams = new JSONObject();
        JSONObject entryObject = new JSONObject()
                .put("level", getParams().getLevel().toString().toLowerCase(Locale.ROOT))
                .put("text", getParams().getMessageText())
                .put("source", getParams().getSource().toString().toLowerCase(Locale.ROOT))
                .put("timestamp", getParams().getTimestamp());

        if (getParams().getArguments() != null && getParams().getArguments().length > 0) {
            entryObject.put("arguments", handleArgument(getParams().getArguments()));
        }
        jsonParams.put("entry", entryObject);

        return super.toJSONObject().put("params", jsonParams);
    }

    private Object handleArgument(Object argument) throws JSONException {
        if (argument instanceof Map) {
            JSONObject jsonObject = new JSONObject();
            Map<?, ?> map = (Map<?, ?>) argument;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                jsonObject.put(entry.getKey().toString(), handleArgument(entry.getValue()));
            }
            return jsonObject;
        } else if (argument instanceof List) {
            JSONArray jsonArray = new JSONArray();
            List<?> list = (List<?>) argument;
            for (Object item : list) {
                jsonArray.put(handleArgument(item));
            }
            return jsonArray;
        } else if (argument.getClass().isArray()) {
            JSONArray jsonArray = new JSONArray();
            int length = Array.getLength(argument);
            for (int i = 0; i < length; i++) {
                jsonArray.put(handleArgument(Array.get(argument, i)));
            }
            return jsonArray;
        } else {
            return argument;
        }
    }

}


