/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.action;

import com.amazon.apl.viewhost.internal.message.action.SendUserEventRequestImpl;
import com.amazon.apl.viewhost.message.BaseMessage;

import java.util.Map;

/**
 * Action message triggered when an APL document issues a SendEvent command.
 *
 * @see https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-standard-commands.html#sendevent-command
 * @see https://developer.amazon.com/en-US/docs/alexa/alexa-voice-service/presentation-apl.html#userevent
 */
public abstract class SendUserEventRequest extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the SendUserEventRequest type
     */
    public static SendUserEventRequest create(ActionMessage message) {
        return new SendUserEventRequestImpl(message);
    }

    /**
     * Array of values specified in the arguments property of the SendEvent command.
     */
    public abstract Object[] getArguments();

    /**
     * Information about the APL component and event handler (if applicable) that was the source
     * of this event.
     */
    public abstract Map<String, Object> getSource();

    /**
     * Value of each component identified in the components property of the SendEvent command.
     */
    public abstract Map<String, Object> getComponents();

    /**
     * Additional runtime-specific properties provided by the APL document.
     */
    public abstract Map<String, Object> getFlags();

    /**
     * Informs the view host that the runtime has successfully performed the required action.
     *
     * @return true if the response was successfully routed and false if the view host consumer has
     * gone away, in which case the response will be discarded.
     */
    public abstract boolean succeed();

    /**
     * Informs the view host that the runtime has failed to perform the required action.
     *
     * @param reason a human-readable reason suitable for logging
     * @return true if the response was successfully routed and false if the view host consumer has
     * gone away, in which case the response will be discarded.
     */
    public abstract boolean fail(String reason);
}
