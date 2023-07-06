/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;

/**
 * Internal implementation of the prepared document
 */
class PreparedDocumentImpl extends PreparedDocument {
    private final DocumentHandleImpl mDocument;

    public PreparedDocumentImpl(DocumentHandleImpl document) {
        mDocument = document;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public boolean hasToken() {
        return false;
    }

    @Override
    public String getToken() {
        return null;
    }

    @Override
    public String getUniqueID() {
        return null;
    }

    @Override
    public DocumentHandle getHandle() {
        return mDocument;
    }
}
