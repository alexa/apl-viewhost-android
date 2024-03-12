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

public final class ViewExecuteCommandsCommandResponse extends ViewDomainCommandResponse {
    private static final String TAG = ViewExecuteCommandsCommandResponse.class.getSimpleName();
    private final Result mResult;

    public ViewExecuteCommandsCommandResponse(int id, String sessionId, ExecuteCommandStatus status) {
        super(id, sessionId);
        mResult = new Result(status.toString());
    }

    private Result getResult() {
        return mResult;
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
        Log.i(TAG, "Serializing " + CommandMethod.VIEW_EXECUTE_COMMANDS + " response object");
        return super.toJSONObject().put("result", new JSONObject()
                .put("status", getResult().getStatus()));
    }
}
