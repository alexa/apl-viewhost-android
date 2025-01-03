/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models;

import com.amazon.apl.devtools.models.common.Request;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandMethodUtil;

import org.json.JSONException;
import org.json.JSONObject;

public final class RequestHeader extends Request {
    public RequestHeader(CommandMethodUtil commandMethodUtil, JSONObject obj) throws JSONException, DTException {
        super(commandMethodUtil.parseMethod(obj.getString("method")), obj);
    }
}
