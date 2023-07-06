/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.notification;

import com.amazon.apl.viewhost.message.GenericMessage;

/**
 * Notification messages are informational messages published by the view host in a send-and-forget
 * manner. The view host does not require the runtime to do anything specific with these messages,
 * but the runtime may trigger its own business logic.
 */
public abstract class NotificationMessage extends GenericMessage {
}
