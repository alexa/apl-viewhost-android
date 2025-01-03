/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.notification;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.message.notification.ScreenLockStatusChanged;

/**
 * Internal implementation for screen lock status changed updates.
 */
public class ScreenLockStatusChangedImpl extends ScreenLockStatusChanged {
    @NonNull
    private final NotificationMessage mNotificationMessage;

    public ScreenLockStatusChangedImpl(NotificationMessage message) {
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
    public boolean hasScreenLockStatusChanged() {
        return mNotificationMessage.getPayload().decodeKeyedContainer().decodeSingleValue("isLocked").decodeBoolean();
    }
}
