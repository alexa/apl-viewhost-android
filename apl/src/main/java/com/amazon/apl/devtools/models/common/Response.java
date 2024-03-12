/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Response implements IResponseParser {
    private final int mId;

    protected Response(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject().put("id", getId());
    }
}
