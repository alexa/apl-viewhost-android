/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.action;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.internal.DecodableShim;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.primitives.Decodable;

import java.util.Map;

/**
 * Internal implementation of FetchDataRequest
 */
public class FetchDataRequestImpl extends FetchDataRequest {
    @NonNull
    private final ActionMessage mActionMessage;

    public FetchDataRequestImpl(ActionMessage message) {
        mActionMessage = message;
    }

    @Override
    public int getId() {
        return mActionMessage.getId();
    }

    @Override
    public DocumentHandle getDocument() {
        return mActionMessage.getDocument();
    }

    @Override
    public String getDataType() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.type;
    }

    @Override
    public Map<String, Object> getParameters() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.parameters;
    }

    @Override
    public boolean succeed() {
        return mActionMessage.succeed();
    }

    @Override
    public boolean fail(String reason) {
        return mActionMessage.fail(reason);
    }

    public static class Payload extends DecodableShim {
        public String type;
        public Map<String, Object> parameters;
    }
}
