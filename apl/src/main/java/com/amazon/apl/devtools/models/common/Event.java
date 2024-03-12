/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import com.amazon.apl.devtools.enums.EventMethod;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Event implements IResponseParser {
    private final EventMethod mMethod;
    private final String mSessionId;

    protected Event(EventMethod mMethod, String mSessionId) {
        this.mMethod = mMethod;
        this.mSessionId = mSessionId;
    }

    public EventMethod getMethod() {
        return mMethod;
    }

    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject().put("method", getMethod().toString())
                .put("sessionId", getSessionId());
    }
}
