/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.notification;

import com.amazon.apl.viewhost.internal.message.notification.ScreenLockStatusChangedImpl;
import com.amazon.apl.viewhost.message.BaseMessage;

/**
 * Notification message triggered when the screen lock status has changed
 */
public abstract class ScreenLockStatusChanged extends BaseMessage {

    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the ScreenLockStatusChanged
     */
    public static ScreenLockStatusChanged create(NotificationMessage message) {
        return new ScreenLockStatusChangedImpl(message);
    }

    /**
     * Boolean to indicate if the screen lock status has changed.
     * @return true/false
     */
    public abstract boolean hasScreenLockStatusChanged();
}
