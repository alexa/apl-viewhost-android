/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.target;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.TargetDomainCommandRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class DetachFromTargetModelRequest extends TargetDomainCommandRequest<DetachFromTargetCommandResponse> {
    private final Params mParams;

    protected DetachFromTargetModelRequest(JSONObject obj) throws JSONException {
        super(CommandMethod.TARGET_DETACH_FROM_TARGET, obj);
        mParams = new Params(obj.getJSONObject("params").getString("sessionId"));
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final String mSessionId;

        private Params(String sessionId) {
            mSessionId = sessionId;
        }

        public String getSessionId() {
            return mSessionId;
        }
    }
}

