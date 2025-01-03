/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.common.BoundObject;

/**
 * This is the peer java class for DocumentManager class in cpp.
 */
@Keep
public class DocumentManager extends BoundObject {

    private final RootConfig mConfig;
    private static final String TAG = "DocumentManager";
    private final EmbeddedDocumentFactory mEmbeddedDocumentFactory;
    private final Handler mHandler;
    private final ITelemetryProvider mTelemetryProvider;

    private static final String EMBEDDED_DOCUMENT_COUNT = "embeddedDocumentCount";

    private final int mEmbeddedDocumentCountMetric;

    public DocumentManager(final EmbeddedDocumentFactory embeddedDocumentFactory, final Handler handler, final ITelemetryProvider telemetryProvider, RootConfig rootConfig) {
        mEmbeddedDocumentFactory = embeddedDocumentFactory;
        mHandler = handler;
        mTelemetryProvider = telemetryProvider;
        mEmbeddedDocumentCountMetric = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, EMBEDDED_DOCUMENT_COUNT, ITelemetryProvider.Type.COUNTER);
        mConfig = rootConfig;
        long handle = nCreate();
        bind(handle);
    }

    public void requestEmbeddedDocument(final EmbeddedDocumentRequestProxy embeddedDocumentRequestProxy) {
        mTelemetryProvider.incrementCount(mEmbeddedDocumentCountMetric);
        EmbeddedDocumentRequestImpl request = new EmbeddedDocumentRequestImpl(embeddedDocumentRequestProxy, mHandler, mTelemetryProvider, mConfig);
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
