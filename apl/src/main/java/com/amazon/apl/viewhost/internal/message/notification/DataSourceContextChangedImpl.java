/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal.message.notification;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.notification.DataSourceContextChanged;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;

/**
 * Internal implementation of DataSourceContextChanged
 */
public class DataSourceContextChangedImpl extends DataSourceContextChanged {
    @NonNull
    private final NotificationMessage mNotificationMessage;

    public DataSourceContextChangedImpl(NotificationMessage message) {
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
