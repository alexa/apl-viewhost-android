/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.document;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.DocumentDomainRequest;
import com.amazon.apl.devtools.models.common.DocumentDomainResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class DocumentCommandRequestModel extends DocumentDomainRequest<DocumentDomainResponse> {
    private final CommandMethod mMethod;
    private final String mSessionId;
    private final JSONObject mParams;

    protected DocumentCommandRequestModel(CommandMethod method, JSONObject obj) throws JSONException {
        super(method, obj);
        mMethod = method;
        mSessionId = obj.getString("sessionId");
        mParams = obj.has("params") && !obj.isNull("params") ?
                obj.getJSONObject("params") :
                new JSONObject();
    }

    public String getStringMethod() {
        return mMethod.toString();
    }

    public String getSessionId() {
        return mSessionId;
    }

    public JSONObject getParams() {
        return mParams;
    }
}
