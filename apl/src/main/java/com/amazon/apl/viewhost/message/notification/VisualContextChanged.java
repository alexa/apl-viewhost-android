/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.notification;

import com.amazon.apl.viewhost.internal.message.notification.VisualContextChangedImpl;
import com.amazon.apl.viewhost.message.BaseMessage;

/**
 * Notification message triggered when the visual context has changed
 */
public abstract class VisualContextChanged extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the VisualContextChanged type
     */
    public static VisualContextChanged create(NotificationMessage message) {
        return new VisualContextChangedImpl(message);
    }
}
