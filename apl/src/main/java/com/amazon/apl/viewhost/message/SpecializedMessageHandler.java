/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message;

import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.message.action.OpenURLRequest;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.message.notification.DataSourceContextChanged;
import com.amazon.apl.viewhost.message.notification.DocumentStateChanged;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.message.notification.ScreenLockStatusChanged;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;

/**
 * Convenience processor for generic messages, which calls specialized handlers for each of the
 * known message types, which the runtime can override as needed.
 */
public class SpecializedMessageHandler implements MessageHandler {
    @Override
    public boolean handleNotification(final NotificationMessage message) {
        switch (message.getType()) {
            case "DataSourceContextChanged":
                return handleDataSourceContextChanged(DataSourceContextChanged.create(message));
            case "DocumentStateChanged":
                return handleDocumentStateChanged(DocumentStateChanged.create(message));
            case "ScreenLockStatusChanged":
                return handleScreenLockStatusChanged(ScreenLockStatusChanged.create(message));
            case "VisualContextChanged":
                return handleVisualContextChanged(VisualContextChanged.create(message));
        }
        return false;
    }

    /**
     * Handle an DataSourceContextChanged notification message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleDataSourceContextChanged(final DataSourceContextChanged message) {
        return false;
    }

    /**
     * Handles any ScreenLockStatusChanged related notifications.
     *
     *  @param message The specialized message
      * @return the runtime should return true if has handled this message, or false to allow the
     *  message to be offered to the next message handler (if available)
     */
    public boolean handleScreenLockStatusChanged(final ScreenLockStatusChanged message) {
        return false;
    }

    /**
     * Handle an DocumentStateChanged notification message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleDocumentStateChanged(final DocumentStateChanged message) {
        return false;
    }

    /**
     * Handle an VisualContextChanged notification message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleVisualContextChanged(final VisualContextChanged message) {
        return false;
    }

    @Override
    public boolean handleAction(final ActionMessage message) {
        switch (message.getType()) {
            case "FetchDataRequest":
                return handleFetchDataRequest(FetchDataRequest.create(message));
            case "OpenURLRequest":
                return handleOpenURLRequest(OpenURLRequest.create(message));
            case "ReportRuntimeErrorRequest":
                return handleReportRuntimeErrorRequest(ReportRuntimeErrorRequest.create(message));
            case "SendUserEventRequest":
                return handleSendUserEventRequest(SendUserEventRequest.create(message));
        }
        return false;
    }

    /**
     * Handle an FetchDataRequest action message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleFetchDataRequest(final FetchDataRequest message) {
        return false;
    }

    /**
     * Handle an OpenURLRequest action message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleOpenURLRequest(final OpenURLRequest message) {
        return false;
    }

    /**
     * Handle a ReportRuntimeErrorRequest action message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleReportRuntimeErrorRequest(final ReportRuntimeErrorRequest message) {
        return false;
    }

    /**
     * Handle a SendUserEventRequest action message.
     *
     * @param message The specialized message
     * @return the runtime should return true if has handled this message, or false to allow the
     * message to be offered to the next message handler (if available)
     */
    public boolean handleSendUserEventRequest(final SendUserEventRequest message) {
        return false;
    }
}
