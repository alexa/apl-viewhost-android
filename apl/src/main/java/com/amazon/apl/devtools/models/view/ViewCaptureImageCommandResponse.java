/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;

import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.ViewDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public final class ViewCaptureImageCommandResponse extends ViewDomainCommandResponse {
    private static final String TAG = ViewCaptureImageCommandResponse.class.getSimpleName();
    private final Result mResult;

    public ViewCaptureImageCommandResponse(int id, String sessionId, int height, int width,
                                           String type, String data) {
        super(id, sessionId);
        mResult = new Result(height, width, type, data);
    }

    public Result getResult() {
        return mResult;
    }

    private static class Result {
        private final int mHeight;
        private final int mWidth;
        private final String mType;
        private final String mData;

        public Result(int height, int width, String type, String data) {
            mHeight = height;
            mWidth = width;
            mType = type;
            mData = data;
        }

        public int getHeight() {
            return mHeight;
        }

        public int getWidth() {
            return mWidth;
        }

        public String getType() {
            return mType;
        }

        public String getData() {
            return mData;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + CommandMethod.VIEW_CAPTURE_IMAGE + " response object");
        return super.toJSONObject().put("result", new JSONObject()
                .put("height", getResult().getHeight())
                .put("width", getResult().getWidth())
                .put("type", getResult().getType())
                .put("data", getResult().getData()));
    }
}
