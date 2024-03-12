/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.target;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.TargetDomainCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class TargetAttachToTargetCommandRequestModel
        extends TargetDomainCommandRequest<TargetAttachToTargetCommandResponse> {
    private final Params mParams;

    protected TargetAttachToTargetCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.TARGET_ATTACH_TO_TARGET, obj);
        mParams = new Params(obj.getJSONObject("params").getString("targetId"));
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final String mTargetId;

        private Params(String targetId) {
            mTargetId = targetId;
        }

        public String getTargetId() {
            return mTargetId;
        }
    }
}
