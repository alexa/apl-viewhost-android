/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.primitives.Decodable;

/**
 * Represents a document-related message sent by the viewhost to the runtime.
 */
public class Message {
    private final DocumentHandle mDocumentHandle;
    private final Decodable mPayload;

    public Message(final DocumentHandle document, final Decodable payload) {
        mDocumentHandle = document;
        mPayload = payload;
    }

    /**
     * @return A handle to the document from which this message originated.
     */
    public DocumentHandle getDocument() {
        return mDocumentHandle;
    }

    /**
     * @return the payload of the message that can be decoded.
     */
    public Decodable getPayload() {
        return mPayload;
    }
}
