/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message.action;

import com.amazon.apl.viewhost.internal.message.action.FetchDataRequestImpl;
import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.primitives.Decodable;

import java.util.Map;

/**
 * Action message triggered when an APL requests dynamic data
 *
 * @see https://developer.amazon.com/en-US/docs/alexa/alexa-voice-service/presentation-apl.html#loadindexlistdata
 * @see https://developer.amazon.com/en-US/docs/alexa/alexa-voice-service/presentation-apl.html#loadtokenlistdata
 */
public abstract class FetchDataRequest extends BaseMessage {
    /**
     * Create an instance of this specialized message given the appropriate generic type
     *
     * @param message The action message of the FetchDataRequest type
     */
    public static FetchDataRequest create(ActionMessage message) {
        return new FetchDataRequestImpl(message);
    }

    /**
     * Get the type of data being requested. Can be one of:
     * - `DYNAMIC_INDEX_LIST`: Requesting more data for a list that uses an index to keep track
     * of the items.
     * - `DYNAMIC_TOKEN_LIST`: Requesting more data for a list that uses a token to keep track of
     * the items.
     *
     * @return the requested data type
     */
    public abstract String getDataType();

    /**
     * @return Type-specific parameters that are related to the type of data being requested
     */
    public abstract Map<String, Object> getParameters();

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
