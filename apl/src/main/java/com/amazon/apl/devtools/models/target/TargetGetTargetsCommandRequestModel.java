/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.target;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.TargetDomainCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class TargetGetTargetsCommandRequestModel
        extends TargetDomainCommandRequest<TargetGetTargetsCommandResponse> {
    protected TargetGetTargetsCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.TARGET_GET_TARGETS, obj);
    }
}
