/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.notification;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.notification.DocumentStateChanged;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.primitives.decoder.KeyedContainerDecoder;

/**
 * Internal implementation of DocumentStateChanged
 */
public class DocumentStateChangedImpl extends DocumentStateChanged {
    @NonNull
    private final NotificationMessage mNotificationMessage;

    public DocumentStateChangedImpl(NotificationMessage message) {
        mNotificationMessage = message;
    }

    @Override
    public int getId() {
        return mNotificationMessage.getId();
    }

    @Override
    public DocumentHandle getDocument() {
        return mNotificationMessage.getDocument();
    }

    @Override
    public String getState() {
        return mNotificationMessage.getPayload().decodeKeyedContainer().decodeSingleValue("state").decodeString();
    }

    @Override
    public String getReason() {
        KeyedContainerDecoder container = mNotificationMessage.getPayload().decodeKeyedContainer();
        if (!container.hasKey("reason")) {
            return null;
        }
        return container.decodeSingleValue("reason").decodeString();
    }
}
