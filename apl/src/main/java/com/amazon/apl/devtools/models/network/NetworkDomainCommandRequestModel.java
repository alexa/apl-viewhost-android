/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.network;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.NetworkDomainRequest;
import com.amazon.apl.devtools.models.common.NetworkDomainResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkDomainCommandRequestModel extends NetworkDomainRequest<NetworkDomainResponse> {
    private final String mSessionId;
    private final JSONObject mParams;

    protected NetworkDomainCommandRequestModel(CommandMethod method, JSONObject obj) throws JSONException {
        super(method, obj);
        mSessionId = obj.getString("sessionId");
        mParams = obj.has("params") && !obj.isNull("params") ?
                obj.getJSONObject("params") :
                new JSONObject();
    }

    public String getSessionId() {
        return mSessionId;
    }

    public JSONObject getParams() {
        return mParams;
    }
}
