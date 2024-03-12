/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.target;

import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.TargetDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public final class TargetAttachToTargetCommandResponse extends TargetDomainCommandResponse {
    private static final String TAG = TargetAttachToTargetCommandResponse.class.getSimpleName();
    private final Result mResult;

    public TargetAttachToTargetCommandResponse(int id, String sessionId) {
        super(id);
        mResult = new Result(sessionId);
    }

    public Result getResult() {
        return mResult;
    }

    private static class Result {
        private final String mSessionId;

        public Result(String sessionId) {
            mSessionId = sessionId;
        }

        public String getSessionId() {
            return mSessionId;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + CommandMethod.TARGET_ATTACH_TO_TARGET + " response object");
        return super.toJSONObject().put("result", new JSONObject()
                .put("sessionId", getResult().getSessionId()));
    }
}
