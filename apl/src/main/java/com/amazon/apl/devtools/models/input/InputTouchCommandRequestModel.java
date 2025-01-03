/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.input;

import android.os.SystemClock;
import android.view.MotionEvent;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.common.InputDomainCommandResponse;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InputTouchCommandRequestModel extends SessionCommandRequest<InputDomainCommandResponse> {
    private final Params mParams;
    protected InputTouchCommandRequestModel(CommandMethod method,
                                            JSONObject obj,
                                            CommandRequestValidator commandRequestValidator,
                                            DTConnection connection) throws JSONException, DTException {
        super(method, obj, commandRequestValidator, connection);
        try {
            JSONObject paramsJsonObject = obj.getJSONObject("params");
            JSONArray eventsJsonArray = paramsJsonObject.getJSONArray("events");
            mParams = new Params();
            for (int i = 0; i < eventsJsonArray.length(); i++) {
                JSONObject eventJson = eventsJsonArray.getJSONObject(i);
                Params.InputEvent event = new Params.InputEvent(eventJson.optString("type"),
                        eventJson.optInt("x"),
                        eventJson.optInt("y"),
                        eventJson.optLong("delay", 0L));
                mParams.addEvent(event);
            }
        } catch (JSONException e) {
            throw new DTException(getId(), DTError.UNKNOWN_ERROR.getErrorCode(),
                    "Malformed input event", e);
        }
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        public static class InputEvent {
            private final String mType;
            private final int mX;
            private final int mY;
            private final long mDelay;

            public InputEvent(String type, int x, int y, long delay) {
                mType = type;
                mX = x;
                mY = y;
                mDelay = delay;
            }

            public String getType() {
                return mType;
            }

            public int getX() {
                return mX;
            }

            public int getY() {
                return mY;
            }

            public long getDelay() {
                return mDelay;
            }
        }

        private List<InputEvent> mEvents = new ArrayList<>();

        public List<MotionEvent> getEvents(float displayRefreshRate) {
            long downTime = SystemClock.uptimeMillis();
            long eventTime = downTime;
            int metaState = 0;
            List<MotionEvent> motionEvents = new ArrayList<>();
            for (InputEvent inputEvent : mEvents) {
                if ("down".equals(inputEvent.getType())) {
                    // Set downTime to the last down event
                    downTime = SystemClock.uptimeMillis();
                    motionEvents.add(MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_DOWN,
                            inputEvent.getX(),
                            inputEvent.getY(),
                            metaState
                    ));
                } else if ("move".equals(inputEvent.getType())) {
                    motionEvents.add(MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_MOVE,
                            inputEvent.getX(),
                            inputEvent.getY(),
                            metaState
                    ));
                } else {
                    motionEvents.add(MotionEvent.obtain(MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_UP,
                            inputEvent.getX(),
                            inputEvent.getY(),
                            metaState
                    )));
                }
                // Increment eventTime by the delay or the default frame time of the system.
                long eventTimeIncrement = inputEvent.getDelay() > 0 ? inputEvent.getDelay() : (long)(1000 / displayRefreshRate);
                eventTime += eventTimeIncrement;
            }
            return motionEvents;
        }

        public void addEvent(InputEvent event) {
            mEvents.add(event);
        }
    }
}
