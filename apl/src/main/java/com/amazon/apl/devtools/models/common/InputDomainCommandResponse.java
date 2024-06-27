/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public class InputDomainCommandResponse extends Response {
    private final String mSessionId;
    private final Result mResult;
    public InputDomainCommandResponse(int id, String sessionId, boolean status) {
        super(id);
        mSessionId = sessionId;
        mResult = new Result(status? "success" : "failure");
    }

    public InputDomainCommandResponse(int id, String sessionId) {
        super(id);
        mSessionId = sessionId;
        mResult = null;
    }

    public String getSessionId() {
        return mSessionId;
    }

    private static class Result {
        private final String mStatus;

        public Result(String status) {
            mStatus = status;
        }

        public String getStatus() {
            return mStatus;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        if (mResult == null) {
            return super.toJSONObject().put("sessionId", getSessionId());
        }
        return super.toJSONObject().put("sessionId", getSessionId()).put("result",
                new JSONObject().put("status", mResult.getStatus()));
    }
}
