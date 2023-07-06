/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;

import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.EmbeddedDocumentResponse;

public class EmbeddedDocumentRequestImpl implements EmbeddedDocumentFactory.EmbeddedDocumentRequest, DocumentStateChangeListener{
    private static final String TAG = "EmbeddedDocumentRequestImpl";
    private String mSource;
    private boolean mIsVisualContextConnected;
    private static final String EMPTY = "";
    private final EmbeddedDocumentRequestProxy mEmbeddedDocumentRequestProxy;
    private final Handler mHandler;
    private DocumentHandleImpl mDocumentHandle;

    EmbeddedDocumentRequestImpl(EmbeddedDocumentRequestProxy embeddedDocumentRequestProxy,
                                Handler handler) {
        mSource = EMPTY;
        mEmbeddedDocumentRequestProxy = embeddedDocumentRequestProxy;
        mHandler = handler;
        mDocumentHandle = null;
        // We assume that documents are separate unless we're told otherwise
        mIsVisualContextConnected = false;
    }
    public void setSource(String source) {
        mSource = source;
    }

    /**
     * @param isVisualContextConnected true for cases when requested document origin is the same as requesting one, false otherwise
     */
    public void setIsVisualContextConnected(boolean isVisualContextConnected) {
        mIsVisualContextConnected = isVisualContextConnected;
    }

    @Override
    public String getSource() {
        return mSource;
    }

    @Override
    public DocumentHandle getRequestingDocument() {
        return mDocumentHandle;
    }

    /**
     * Added for testing purpose.
     * @param documentHandle
     */
    @VisibleForTesting
    void setDocumentHandle(final DocumentHandleImpl documentHandle) {
        mDocumentHandle = documentHandle;
    }

    @Override
    public void resolve(PreparedDocument preparedDocument) {
        EmbeddedDocumentResponse response = EmbeddedDocumentResponse.builder()
                .preparedDocument(preparedDocument)
                .visualContextAttached(false)
                .build();
        resolve(response);
    }

    @Override
    public void resolve(EmbeddedDocumentResponse response) {
        mIsVisualContextConnected = response.isVisualContextAttached();
        mDocumentHandle = (DocumentHandleImpl) response.getPreparedDocument().getHandle();
        if (mDocumentHandle != null) {
            mDocumentHandle.registerStateChangeListener(this);
        }
    }

    @Override
    public void fail(String reason) {
        mHandler.post(() -> mEmbeddedDocumentRequestProxy.failure(reason));
    }

    @Override
    public void onDocumentStateChanged(DocumentState state) {
        // when the document state is prepared then content is done so call success
        if (state == DocumentState.PREPARED) {
            mHandler.post(() -> {
                if (mDocumentHandle != null) {
                    ExtensionMediator mediator = mDocumentHandle.getExtensionMediator();
                    long mediatorNativeHandle = mediator != null ? mediator.getNativeHandle() : 0;
                    long documentConfigHandle = nCreateDocumentConfig(mediatorNativeHandle);
                    mDocumentHandle.setDocumentConfig(new DocumentConfig(documentConfigHandle));

                    DocumentContext documentContext =
                            mEmbeddedDocumentRequestProxy.success(mDocumentHandle.getContent().getNativeHandle(), mIsVisualContextConnected, documentConfigHandle);
                    mDocumentHandle.setDocumentContext(documentContext);
                } else {
                    mEmbeddedDocumentRequestProxy.failure("document handle is null");
                }
            });
        } else if (state == DocumentState.ERROR) {
            mHandler.post(() -> mEmbeddedDocumentRequestProxy.failure("Content creation error, Document State set to Error"));
        }
    }

    private static native long nCreateDocumentConfig(long mediatorHandle);
}
