/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collection of frame begin and end timestamps
 */
public class FrameStat {
    // Timestamp in nanoseconds since a platform dependent arbitrary time base, marking the beginning of the frame loop.
    long begin;
    // Timestamp in nanoseconds since a platform dependent arbitrary time base, marking the ending of the frame loop. The time base is the same as the one used for begin timestamp.
    long end;

    public FrameStat(long begin, long end) {
        this.begin = begin;
        this.end = end;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("begin", begin);
        jsonObject.put("end", end);
        return jsonObject;
    }
}
