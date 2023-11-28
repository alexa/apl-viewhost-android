/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.amazon.common.BoundObject;

/**
 * Wrapper around Document Manager Request.
 */
@Keep
public class EmbeddedDocumentRequestProxy extends BoundObject {

    public EmbeddedDocumentRequestProxy(long nativeHandle) {
        bind(nativeHandle);
    }

    @Nullable
    public DocumentContext success(long contentHandle, boolean isVisualContextConnected, long documentConfigHandle) {
        long documentContextHandle = nSuccess(getNativeHandle(), contentHandle, isVisualContextConnected, documentConfigHandle);
        if (documentContextHandle == 0) {
            return null;
        }
        DocumentContext documentContext = new DocumentContext(documentContextHandle);
        return documentContext;
    }

    public void failure(String message) {
        nFailure(getNativeHandle(), message);
    }

    public String getRequestUrl() {
        return nGetRequestUrl(getNativeHandle());
    }

    private native String nGetRequestUrl(long nativeHandle);
    private native long nSuccess(long nativeHandle, long contentHandle, boolean isVisualContextConnected, long documentConfigHandle);
    private native void nFailure(long nativeHandle, String message);
}
