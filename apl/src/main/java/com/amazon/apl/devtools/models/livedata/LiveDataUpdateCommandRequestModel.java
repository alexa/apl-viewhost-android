/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.livedata;

import com.amazon.apl.android.LiveArray;
import com.amazon.apl.android.LiveData;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.common.ViewDomainCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class LiveDataUpdateCommandRequestModel
        extends ViewDomainCommandRequest<LiveDataUpdateCommandResponse> {
    private final Params mParams;

    protected LiveDataUpdateCommandRequestModel(JSONObject obj) throws JSONException, DTException {
        super(CommandMethod.LIVE_DATA_UPDATE, obj);
        try {
            mParams = new Params(obj.getJSONObject("params"));
        } catch (JSONException e) {
            throw new DTException(getId(), DTError.INVALID_DOCUMENT.getErrorCode(),
                    "Invalid commands document", e);
        }
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final String mName;
        private final List<LiveArray.Update> mOperations = new ArrayList<>();

        private Params(JSONObject params) throws JSONException {
            mName = params.getString("name");
            JSONArray operations = params.getJSONArray("operations");
            for (int i = 0; i < operations.length(); i++) {
                JSONObject operation = operations.getJSONObject(i);
                mOperations.add(
                        new LiveData.Update(
                                operation.getString("type"),
                                operation.optInt("index", 0),
                                operation.optString("key", ""),
                                operation.get("value")
                        )
                );
            }
        }

        public String getName() {
            return mName;
        }

        public List<LiveArray.Update> getOperations() {
            return mOperations;
        }
    }
}
