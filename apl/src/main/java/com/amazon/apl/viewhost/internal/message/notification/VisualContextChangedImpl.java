/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.notification;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;

/**
 * Internal implementation of VisualContextChanged
 */
public class VisualContextChangedImpl extends VisualContextChanged {
    @NonNull
    private final NotificationMessage mNotificationMessage;

    public VisualContextChangedImpl(NotificationMessage message) {
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
}
