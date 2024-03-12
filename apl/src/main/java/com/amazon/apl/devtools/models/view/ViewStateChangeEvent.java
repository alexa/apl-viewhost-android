/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;

import android.util.Log;

import com.amazon.apl.devtools.enums.EventMethod;
import com.amazon.apl.devtools.enums.ViewState;
import com.amazon.apl.devtools.models.common.Event;

import org.json.JSONException;
import org.json.JSONObject;

public final class ViewStateChangeEvent extends Event {
    private static final String TAG = ViewStateChangeEvent.class.getSimpleName();
    private final Params mParams;

    public ViewStateChangeEvent(String mSessionId, ViewState state, int documentId,
                                double timestamp) {
        super(EventMethod.VIEW_STATE_CHANGE, mSessionId);
        mParams = new Params(state, documentId, timestamp);
    }

    public Params getParams() {
        return mParams;
    }

    private static class Params {
        private final ViewState mState;
        private final int mDocumentId;
        private final double mTimestamp;

        public Params(ViewState state, int documentId, double timestamp) {
            mState = state;
            mDocumentId = documentId;
            mTimestamp = timestamp;
        }

        public ViewState getState() {
            return mState;
        }

        public int getDocumentId() {
            return mDocumentId;
        }

        public double getTimestamp() {
            return mTimestamp;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.VIEW_STATE_CHANGE + " event object");
        return super.toJSONObject().put("params", new JSONObject()
                .put("state", getParams().getState())
                .put("documentId", getParams().getDocumentId())
                .put("timestamp", getParams().getTimestamp()));
    }
}
