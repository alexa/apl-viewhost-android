/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.notification;

import com.amazon.apl.viewhost.internal.message.notification.DataSourceContextChangedImpl;
import com.amazon.apl.viewhost.message.BaseMessage;

/**
 * Notification message triggered when the data source context has changed
 */
public abstract class DataSourceContextChanged extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the DataSourceContextChanged type
     */
    public static DataSourceContextChanged create(NotificationMessage message) {
        return new DataSourceContextChangedImpl(message);
    }
}
