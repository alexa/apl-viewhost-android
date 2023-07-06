/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.common.BoundObject;

/**
 * This is the peer java class for DocumentManager class in cpp.
 */
@Keep
public class DocumentManager extends BoundObject {
    private static final String TAG = "DocumentManager";
    private final EmbeddedDocumentFactory mEmbeddedDocumentFactory;
    private final Handler mHandler;

    public DocumentManager(final EmbeddedDocumentFactory embeddedDocumentFactory, final Handler handler) {
        mEmbeddedDocumentFactory = embeddedDocumentFactory;
        mHandler = handler;
        long handle = nCreate();
        bind(handle);
    }

    public void requestEmbeddedDocument(final EmbeddedDocumentRequestProxy embeddedDocumentRequestProxy) {
        EmbeddedDocumentRequestImpl request = new EmbeddedDocumentRequestImpl(embeddedDocumentRequestProxy, mHandler);
        String sourceUrl = embeddedDocumentRequestProxy.getRequestUrl();
        if(TextUtils.isEmpty(sourceUrl)) {
            Log.e(TAG, "requestEmbeddedDocument: sourceUrl is null");
            return;
        }
        request.setSource(sourceUrl);
        mEmbeddedDocumentFactory.onDocumentRequested(request);
    }

    private native long nCreate();
}
