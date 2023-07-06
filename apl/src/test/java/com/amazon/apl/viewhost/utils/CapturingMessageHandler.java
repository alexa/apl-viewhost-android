/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.utils;

import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.message.SpecializedMessageHandler;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.message.action.OpenURLRequest;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.message.notification.DataSourceContextChanged;
import com.amazon.apl.viewhost.message.notification.DocumentStateChanged;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Handles every specialized message and saves them for later
 */
public class CapturingMessageHandler extends SpecializedMessageHandler {
    public Queue<BaseMessage> queue = new LinkedList<>();

    @Override
    public boolean handleDataSourceContextChanged(final DataSourceContextChanged message) {
        queue.offer(message);
        return true;
    }

    @Override
    public boolean handleDocumentStateChanged(final DocumentStateChanged message) {
        queue.offer(message);
        return true;
    }

    @Override
    public boolean handleFetchDataRequest(final FetchDataRequest message) {
        queue.offer(message);
        return true;
    }

    @Override
    public boolean handleOpenURLRequest(final OpenURLRequest message) {
        queue.offer(message);
        return true;
    }

    @Override
    public boolean handleReportRuntimeErrorRequest(final ReportRuntimeErrorRequest message) {
        queue.offer(message);
        return true;
    }

    @Override
    public boolean handleSendUserEventRequest(final SendUserEventRequest message) {
        queue.offer(message);
        return true;
    }

    @Override
    public boolean handleVisualContextChanged(final VisualContextChanged message) {
        queue.offer(message);
        return true;
    }
}