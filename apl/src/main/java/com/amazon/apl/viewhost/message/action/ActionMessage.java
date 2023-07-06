/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.action;

import com.amazon.apl.viewhost.message.GenericMessage;
import com.amazon.apl.viewhost.primitives.Decodable;

/**
 * Action messages are published by the view host when it expects the runtime to do something
 * specific. The runtime is expected to fulfill action messages to achieve some functionality on
 * behalf of the customer.
 * <p>
 * Upon successfully performing an action, the runtime must notify the view host via the succeed()
 * callback. If success requires a specific response payload, it is provided via the
 * succeed(response) method.
 * <p>
 * Upon failure to perform an action, the runtime calls fail() with a human-readable reason.
 * <p>
 * Implementations of this class are guaranteed to be thread-safe.
 */
public abstract class ActionMessage extends GenericMessage {
    /**
     * Informs the view host that the runtime has successfully performed the required action.
     *
     * @return true if the response was successfully routed and false if the view host consumer has
     * gone away, in which case the response will be discarded.
     */
    public abstract boolean succeed();

    /**
     * Informs the view host that the runtime has successfully performed the required action. This
     * variant allows the runtime to provide a decodable payload, if appropriate.
     *
     * @param payload A specific decodable payload expected in response to the action message
     * @return true if the response was successfully routed and false if the view host consumer has
     * gone away, in which case the response will be discarded.
     */
    public abstract boolean succeed(Decodable payload);

    /**
     * Informs the view host that the runtime has failed to perform the required action.
     *
     * @param reason a human-readable reason suitable for logging
     * @return true if the response was successfully routed and false if the view host consumer has
     * gone away, in which case the response will be discarded.
     */
    public abstract boolean fail(String reason);
}
