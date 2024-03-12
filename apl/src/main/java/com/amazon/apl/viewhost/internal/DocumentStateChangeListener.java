/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import com.amazon.apl.viewhost.DocumentHandle;

/**
 * Listens to the document state change events.
 */
public interface DocumentStateChangeListener {
    void onDocumentStateChanged(DocumentState state, DocumentHandle handle);
}
