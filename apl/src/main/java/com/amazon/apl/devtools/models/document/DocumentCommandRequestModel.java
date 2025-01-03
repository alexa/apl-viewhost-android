/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.document;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.DocumentDomainResponse;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class DocumentCommandRequestModel extends SessionCommandRequest<DocumentDomainResponse> {
    private final JSONObject mParams;

    protected DocumentCommandRequestModel(CommandMethod method, JSONObject obj, CommandRequestValidator commandRequestValidator, DTConnection connection) throws JSONException, DTException {
        super(method, obj, commandRequestValidator, connection);
        mParams = obj.has("params") && !obj.isNull("params") ?
                obj.getJSONObject("params") :
                new JSONObject();
    }

    public JSONObject getParams() {
        return mParams;
    }
}
