/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.notification;

import com.amazon.apl.viewhost.internal.message.notification.DocumentStateChangedImpl;
import com.amazon.apl.viewhost.message.BaseMessage;

/**
 * Notification message triggered when an APL document state changes.
 */
public abstract class DocumentStateChanged extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the DocumentStateChanged type
     */
    public static DocumentStateChanged create(NotificationMessage message) {
        return new DocumentStateChangedImpl(message);
    }

    /**
     * Gets the new document state, which can be one of:
     * - `PREPARED`: The pre-inflation dependencies have been satisfied (imported packages,
     * extensions loaded).
     * - `INFLATED`: The APL engine has inflated all of the components needed for the first frame.
     * - `DISPLAYED`: The APL engine has prepared a visual representations of components needed for
     * the first frame and has handed off instructions to the platform for rendering. This
     * corresponds to when the VUPL clock is stopped.
     * - `FINISHED`: All resources associated with a document have been released and no further
     * interaction is possible.
     * - `ERROR`: The document has resulted in a permanent failure. Please check the `reason` for
     * more details.
     *
     * @return the document state
     */
    public abstract String getState();

    /**
     * @return Human-readable reason for the state change, suitable for logging. Only provided for
     * the ERROR state or null otherwise.
     */
    public abstract String getReason();
}
