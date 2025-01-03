/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.LogDomainResponse;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class LogEnableCommandRequestModel extends SessionCommandRequest<LogDomainResponse> {
    protected LogEnableCommandRequestModel(JSONObject obj, CommandRequestValidator commandRequestValidator, DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.LOG_ENABLE, obj, commandRequestValidator, connection);
    }
}
