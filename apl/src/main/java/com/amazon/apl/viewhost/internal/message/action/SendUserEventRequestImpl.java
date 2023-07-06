/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.action;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.internal.DecodableShim;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;

import java.util.Map;

/**
 * Internal implementation of SendUserEventRequest
 */
public class SendUserEventRequestImpl extends SendUserEventRequest {
    @NonNull
    private final ActionMessage mActionMessage;

    public SendUserEventRequestImpl(ActionMessage message) {
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
    public Object[] getArguments() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.arguments;
    }

    @Override
    public Map<String, Object> getSource() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.source;
    }

    @Override
    public Map<String, Object> getComponents() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.components;
    }

    @Override
    public Map<String, Object> getFlags() {
        Payload payload = (Payload) mActionMessage.getPayload();
        return payload.flags;
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
        public Object[] arguments;
        public Map<String, Object> source;
        public Map<String, Object> components;
        public Map<String, Object> flags;
    }
}
