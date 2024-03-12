/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public class DocumentDomainResponse extends Response {
    private final String mSessionId;
    private final JSONObject mResult;

    public DocumentDomainResponse(int id, String sessionId) {
        this(id, sessionId, null);
    }

    public DocumentDomainResponse(int id, String sessionId, JSONObject result) {
        super(id);
        mSessionId = sessionId;
        mResult = result;
    }

    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        if (mResult == null) {
            return super.toJSONObject().put("sessionId", getSessionId());
        }
        return super.toJSONObject().put("sessionId", getSessionId()).put("result", mResult);
    }
}
