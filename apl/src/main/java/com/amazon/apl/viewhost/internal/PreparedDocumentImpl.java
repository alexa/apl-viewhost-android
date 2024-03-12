/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;

import lombok.NonNull;

/**
 * Internal implementation of the prepared document
 */
class PreparedDocumentImpl extends PreparedDocument {
    @NonNull
    private final DocumentHandleImpl mDocument;

    public PreparedDocumentImpl(DocumentHandleImpl document) {
        mDocument = document;
    }

    @Override
    public boolean isReady() {
        return DocumentState.PREPARED.equals(mDocument.getDocumentState());
    }

    @Override
    public boolean isValid() {
        return getHandle().isValid();
    }

    @Override
    public boolean hasToken() {
        return getToken() != null;
    }

    @Override
    public String getToken() {
        return getHandle().getToken();
    }

    @Override
    public String getUniqueID() {
        return getHandle().getUniqueId();
    }

    @Override
    public DocumentHandle getHandle() {
        return mDocument;
    }
}
