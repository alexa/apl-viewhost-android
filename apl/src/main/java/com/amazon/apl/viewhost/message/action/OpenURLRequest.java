/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.action;

import com.amazon.apl.viewhost.internal.message.action.OpenURLRequestImpl;
import com.amazon.apl.viewhost.message.BaseMessage;

/**
 * on message triggered when an APL document requests to open a URL via the OpenURL command.
 *
 * @see https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-standard
 * -commands.html#open_url_command
 */
public abstract class OpenURLRequest extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the OpenURLRequest type
     */
    public static OpenURLRequest create(ActionMessage message) {
        return new OpenURLRequestImpl(message);
    }

    /**
     * The URL to open.
     */
    public abstract String getSource();

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
