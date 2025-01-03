/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.common.LogDomainResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.log.LogClearCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import org.json.JSONException;
import org.json.JSONObject;

public class LogClearCommandRequest extends LogClearCommandRequestModel {

    public LogClearCommandRequest(CommandRequestValidator commandRequestValidator,
                                  JSONObject obj,
                                  DTConnection connection) throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public LogDomainResponse execute() {
        getSession().clearLog();
        return new LogDomainResponse(getId(), getSessionId());
    }

    @Override
    public void validate() throws DTException {
        if (!getSession().isLogEnabled()) {
            throw new DTException(getId(), DTError.LOG_ALREADY_DISABLED.getErrorCode(),
                    "Log is not enabled");
        }
    }
}
