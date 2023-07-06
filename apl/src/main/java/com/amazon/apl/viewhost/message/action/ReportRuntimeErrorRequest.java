/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.action;

import com.amazon.apl.viewhost.internal.message.action.ReportRuntimeErrorRequestImpl;
import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.primitives.Decodable;

import java.util.List;

/**
 * Action message triggered when the view host encounters one or more runtime errors that should be
 * reported to the skill.
 *
 * @see https://developer.amazon.com/en-US/docs/alexa/alexa-voice-service/presentation-apl.html#runtimeerror
 */
public abstract class ReportRuntimeErrorRequest extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the ReportRuntimeErrorRequest type
     */
    public static ReportRuntimeErrorRequest create(ActionMessage message) {
        return new ReportRuntimeErrorRequestImpl(message);
    }

    /**
     * Return the errors suitable for inclusion in the RuntimeError payload.
     *
     * @return the errors being reported
     */
    public abstract Object[] getErrors();

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
