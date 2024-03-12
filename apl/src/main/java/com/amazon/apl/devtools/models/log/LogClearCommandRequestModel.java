/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.LogDomainRequest;
import com.amazon.apl.devtools.models.common.LogDomainResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class LogClearCommandRequestModel extends LogDomainRequest<LogDomainResponse> {

    protected LogClearCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.LOG_CLEAR, obj);
    }
}
