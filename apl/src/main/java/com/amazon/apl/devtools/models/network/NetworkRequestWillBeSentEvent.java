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

public final class NetworkRequestWillBeSentEvent extends Event {
    private static final String TAG = NetworkRequestWillBeSentEvent.class.getSimpleName();
    private final Params mParams;

    public NetworkRequestWillBeSentEvent(String mSessionId, int requestId,  double timestamp,
                                         String documentURL, String type) {
        super(EventMethod.NETWORK_REQUEST_WILL_BE_SENT, mSessionId);
        mParams = new Params(requestId, timestamp, documentURL, type);
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final int mRequestId;
        private final double mTimestamp;
        private final String mDocumentURL;
        private final String mType;

        public Params(int requestId,  double timestamp,
                      String documentURL, String type) {
            mRequestId = requestId;
            mTimestamp = timestamp;
            mDocumentURL = documentURL;
            mType = type;
        }

        public int getRequestId() {
            return mRequestId;
        }

        public double getTimestamp() {
            return mTimestamp;
        }

        public String getDocumentURL() {
            return mDocumentURL;
        }

        public String getType() {
            return mType;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.NETWORK_REQUEST_WILL_BE_SENT + " event object");
        return super.toJSONObject().put("params", new JSONObject()
                .put("requestId", getParams().getRequestId())
                .put("timestamp", getParams().getTimestamp())
                .put("type", getParams().getType())
                .put("documentURL", getParams().getDocumentURL()));
    }
}
