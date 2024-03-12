/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkDomainResponse extends Response {
    private final String mSessionId;

    public NetworkDomainResponse(int id, String sessionId) {
        super(id);
        mSessionId = sessionId;
    }

    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject().put("sessionId", getSessionId());
    }
}
