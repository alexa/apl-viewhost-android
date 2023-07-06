/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.notification;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.primitives.Decodable;

/**
 * Implement the action message contract
 */
public class NotificationMessageImpl extends NotificationMessage {
    private final int mId;
    @NonNull
    private final DocumentHandle mDocument;
    private final String mType;
    @NonNull
    private final Decodable mPayload;

    public NotificationMessageImpl(int id, DocumentHandle document, String type,
                                   Decodable payload) {
        mId = id;
        mDocument = document;
        mType = type;
        mPayload = payload;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public DocumentHandle getDocument() {
        return mDocument;
    }

    @Override
    public String getType() {
        return mType;
    }

    @Override
    public Decodable getPayload() {
        return mPayload;
    }
}
