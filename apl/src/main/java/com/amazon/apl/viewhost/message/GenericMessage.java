/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message;

import com.amazon.apl.viewhost.primitives.Decodable;

/**
 * Represents message that can be processed generically by examining its type and payload.
 */
public abstract class GenericMessage extends BaseMessage {
    /**
     * @return the type of message
     */
    public abstract String getType();

    /**
     * @return the payload of the message that can be decoded.
     */
    public abstract Decodable getPayload();
}
