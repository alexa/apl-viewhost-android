/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.network;

import android.util.Log;

import com.amazon.apl.devtools.enums.EventMethod;
import com.amazon.apl.devtools.models.common.Event;

import org.json.JSONException;
import org.json.JSONObject;

public final class NetworkLoadingFailedEvent extends Event {
    private static final String TAG = NetworkLoadingFailedEvent.class.getSimpleName();
    private final Params mParams;

    public NetworkLoadingFailedEvent(String mSessionId, int requestId,  double timestamp) {
        super(EventMethod.NETWORK_LOADING_FAILED, mSessionId);
        mParams = new Params(requestId, timestamp);
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final int mRequestId;
        private final double mTimestamp;

        public Params(int requestId,  double timestamp) {
            mRequestId = requestId;
            mTimestamp = timestamp;
        }

        public int getRequestId() {
            return mRequestId;
        }

        public double getTimestamp() {
            return mTimestamp;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.NETWORK_LOADING_FAILED + " event object");
        return super.toJSONObject().put("params", new JSONObject()
                .put("requestId", getParams().getRequestId())
                .put("timestamp", getParams().getTimestamp()));
    }
}
