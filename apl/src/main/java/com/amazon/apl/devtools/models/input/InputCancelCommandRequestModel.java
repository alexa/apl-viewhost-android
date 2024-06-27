/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.input;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.InputDomainCommandRequest;
import com.amazon.apl.devtools.models.common.InputDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class InputCancelCommandRequestModel extends InputDomainCommandRequest<InputDomainCommandResponse> {
    protected InputCancelCommandRequestModel(CommandMethod method, JSONObject obj) throws JSONException {
        super(method, obj);
    }
}
