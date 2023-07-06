/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message;

import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;

/**
 * A message handler handles messages issued by the viewhost. This is typically injected as a
 * dependency from a runtime.
 * <p>
 * The viewhost guarantees that messages will be provided to the handler sequentially, in the order
 * they are published by the viewhost. There is no guarantee however that the message handler will
 * always be invoked from the same thread.
 */
public interface MessageHandler {
    /**
     * Handle a notification mesage.
     *
     * @param message The generic notification message
     * @return the runtime should return true if it has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    boolean handleNotification(final NotificationMessage message);

    /**
     * Handle an action mesage.
     *
     * @param message The generic action message
     * @return the runtime should return true if it has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    boolean handleAction(final ActionMessage message);
}
