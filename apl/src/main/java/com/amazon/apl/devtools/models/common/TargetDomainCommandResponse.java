/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class TargetDomainCommandResponse extends Response {
    protected TargetDomainCommandResponse(int id) {
        super(id);
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject();
    }
}
