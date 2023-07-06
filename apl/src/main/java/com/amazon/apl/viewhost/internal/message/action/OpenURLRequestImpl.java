/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.action;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.OpenURLRequest;

/**
 * Internal implementation of OpenURLRequest
 */
public class OpenURLRequestImpl extends OpenURLRequest {
    @NonNull
    private final ActionMessage mActionMessage;

    public OpenURLRequestImpl(ActionMessage message) {
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
    public String getSource() {
        return mActionMessage.getPayload().decodeKeyedContainer().decodeSingleValue("source").decodeString();
    }

    @Override
    public boolean succeed() {
        return mActionMessage.succeed();
    }

    @Override
    public boolean fail(String reason) {
        return mActionMessage.fail(reason);
    }
}
