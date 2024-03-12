/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.error;

import android.util.Log;

import com.amazon.apl.devtools.models.common.Response;

import org.json.JSONException;
import org.json.JSONObject;

public final class ErrorResponse extends Response {
    private static final String TAG = ErrorResponse.class.getSimpleName();
    private final Error mError;

    public ErrorResponse(DTException e) {
        super(e.getId());
        Log.i(TAG, "Creating error response from DTException");
        mError = new Error(e.getCode(), e.getMessage());
    }

    public ErrorResponse(int id, int code, String message) {
        super(id);
        Log.i(TAG, "Creating error response");
        mError = new Error(code, message);
    }

    private static class Error {
        private final int mCode;
        private final String mMessage;

        private Error(int code, String message) {
            mCode = code;
            mMessage = message;
        }

        public int getCode() {
            return mCode;
        }

        public String getMessage() {
            return mMessage;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject().put("error", new JSONObject()
                .put("code", mError.mCode).put("message", mError.mMessage));
    }
}
