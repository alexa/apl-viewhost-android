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

public final class NetworkLoadingFinishedEvent extends Event {
    private static final String TAG = NetworkLoadingFinishedEvent.class.getSimpleName();
    private final Params mParams;

    public NetworkLoadingFinishedEvent(String mSessionId, int requestId,  double timestamp,
                                         int encodedDataLength) {
        super(EventMethod.NETWORK_LOADING_FINISHED, mSessionId);
        mParams = new Params(requestId, timestamp, encodedDataLength);
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final int mRequestId;
        private final double mTimestamp;
        private final int mEncodedDataLength;

        public Params(int requestId,  double timestamp,
                      int encodedDataLength) {
            mRequestId = requestId;
            mTimestamp = timestamp;
            mEncodedDataLength = encodedDataLength;
        }

        public int getRequestId() {
            return mRequestId;
        }

        public double getTimestamp() {
            return mTimestamp;
        }

        public int getEncodedDataLength() {
            return mEncodedDataLength;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.NETWORK_LOADING_FINISHED + " event object");
        return super.toJSONObject().put("params", new JSONObject()
                .put("encodedDataLength", getParams().getEncodedDataLength())
                .put("requestId", getParams().getRequestId())
                .put("timestamp", getParams().getTimestamp()));
    }
}
