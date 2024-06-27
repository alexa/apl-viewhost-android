/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import com.amazon.apl.devtools.enums.CommandMethod;

import org.json.JSONException;
import org.json.JSONObject;

public class InputDomainCommandRequest <TResponse extends Response> extends Request<TResponse> {
    private final String mSessionId;
    protected InputDomainCommandRequest(CommandMethod method, JSONObject obj) throws JSONException {
        super(method, obj);
        mSessionId = obj.getString("sessionId");
    }

    public String getSessionId() {
        return mSessionId;
    }
}
