/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.livedata;

import android.util.Log;

import com.amazon.apl.devtools.models.common.ViewDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public final class LiveDataUpdateCommandResponse extends ViewDomainCommandResponse {
    private static final String TAG = LiveDataUpdateCommandResponse.class.getSimpleName();

    private final Result mResult;

    public LiveDataUpdateCommandResponse(int id, String sessionId, boolean status){
        super(id, sessionId);
        mResult = new Result(status? "success" : "failure");
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
        Log.i(TAG, "Serializing LiveDataUpdate response object");
        return super.toJSONObject();
    }
}
