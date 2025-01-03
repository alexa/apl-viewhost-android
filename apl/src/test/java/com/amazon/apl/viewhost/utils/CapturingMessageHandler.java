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
import com.amazon.apl.viewhost.message.notification.ScreenLockStatusChanged;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles every specialized message and saves them for later
 */
public class CapturingMessageHandler extends SpecializedMessageHandler {
    public Queue<BaseMessage> queue = new ConcurrentLinkedQueue<>();

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
    public boolean handleScreenLockStatusChanged(final ScreenLockStatusChanged message) {
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

    /**
     * Finds the first message of the specified type in the queue, if any.
     *
     * @param <T> the type of the message
     * @param messageClass the class of message to search for in the queue
     * @return the first instance of the target class found in the queue, or null if none was found
     * @throws IllegalStateException if more than one instance of the target class is found in the queue
     */
    public <T> T findOne(Class<T> messageClass) {
        T result = null;
        for (Object message : queue) {
            if (messageClass.isInstance(message)) {
                if (result == null) {
                    result = messageClass.cast(message);
                } else {
                    throw new IllegalStateException("More than one instance of " + messageClass.getSimpleName() + " found in the queue.");
                }
            }
        }
        return result;
    }

    /**
     * Finds all instances of the specified message type in the queue.
     *
     * @param <T> the type of the target class
     * @param messageClass the class to search for in the queue
     * @return a list of all instances of the target class found in the queue
     */
    public <T> List<T> findAll(Class<T> messageClass) {
        List<T> results = new ArrayList<>();

        for (Object obj : queue) {
            if (messageClass.isInstance(obj)) {
                T instance = messageClass.cast(obj);
                results.add(instance);
            }
        }

        return results;
    }

    /**
     * Returns true if at least one message of the type is in the queue
     *
     * @param <T> the type of the message
     * @param messageClass the class of message to search for in the queue
     * @return the first instance of the target class found in the queue, or null if none was found
     */
    public <T> boolean has(Class<T> messageClass) {
        for (Object message : queue) {
            if (messageClass.isInstance(message)) {
                return true;
            }
        }
        return false;
    }
}
