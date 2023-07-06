/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.action;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.internal.DecodableShim;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.primitives.Decodable;

import java.util.List;
import java.util.Map;

/**
 * Internal implementation of ReportRuntimeErrorRequest
 */
public class ReportRuntimeErrorRequestImpl extends ReportRuntimeErrorRequest {
    @NonNull
    private final ActionMessage mActionMessage;

    public ReportRuntimeErrorRequestImpl(ActionMessage message) {
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
    public Object[] getErrors() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.errors;
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
        public Object[] errors;
    }
}
