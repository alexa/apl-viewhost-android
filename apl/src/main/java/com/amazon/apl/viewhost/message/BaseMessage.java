/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.message;

import com.amazon.apl.viewhost.DocumentHandle;

/**
 * The basic message contract, which consists of an id and a document.
 */
public abstract class BaseMessage {
    /**
     * @return Monotonically increasing value that uniquely identifies a message within the context
     * of a view host instance.
     */
    public abstract int getId();

    /**
     * @return A handle to the document from which this message originated.
     */
    public abstract DocumentHandle getDocument();
}
