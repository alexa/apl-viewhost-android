/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.EmbeddedDocumentResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EmbeddedDocumentRequestImpl implements EmbeddedDocumentFactory.EmbeddedDocumentRequest, DocumentStateChangeListener{
    private static final String TAG = "EmbeddedDocumentRequestImpl";
    private String mSource;
    private boolean mIsVisualContextConnected;
    private static final String EMPTY = "";
    private final EmbeddedDocumentRequestProxy mEmbeddedDocumentRequestProxy;
    private final Handler mHandler;
    private DocumentHandleImpl mDocumentHandle;
    private ITelemetryProvider mTelemetryProvider;
    private static final String PREPARE_EMBEDDED_DOC_COUNT = "prepareEmbeddedDocCount";
    private static final String PREPARE_EMBEDDED_DOC_TIME = "prepareEmbeddedDocTime";
    private final int mPrepareEmbeddedDocCount;
    private final int mPrepareEmbeddedDocTime;

    EmbeddedDocumentRequestImpl(EmbeddedDocumentRequestProxy embeddedDocumentRequestProxy,
                                Handler handler, ITelemetryProvider telemetryProvider) {
        mSource = EMPTY;
        mEmbeddedDocumentRequestProxy = embeddedDocumentRequestProxy;
        mTelemetryProvider = telemetryProvider;
        mHandler = handler;
        mDocumentHandle = null;
        // We assume that documents are separate unless we're told otherwise
        mIsVisualContextConnected = false;
        mPrepareEmbeddedDocCount = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, PREPARE_EMBEDDED_DOC_COUNT, ITelemetryProvider.Type.COUNTER);
        mPrepareEmbeddedDocTime = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, PREPARE_EMBEDDED_DOC_TIME, ITelemetryProvider.Type.TIMER);
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

    /**
     * Handle the success callback for the EmbeddedDocumentRequest
     *
     * @param content
     * @param documentConfigHandle
     */
    private void handleEmbeddedDocumentRequestSuccess(Content content, long documentConfigHandle) {
        long endTime = SystemClock.elapsedRealtime();
        long documentPreparationTime = endTime - mDocumentHandle.getPrepareDocumentStartTime();
        mTelemetryProvider.reportTimer(mPrepareEmbeddedDocTime, TimeUnit.MILLISECONDS, documentPreparationTime);

        DocumentContext documentContext =
                mEmbeddedDocumentRequestProxy.success(content.getNativeHandle(), mIsVisualContextConnected, documentConfigHandle);

        if (documentContext == null) {
            // Host component was released, returning a null DocumentContext
            // Set a terminal state for the DocumentHandle
            Log.w(TAG, "Got a null DocumentContext, dropping request and setting Document State to Finished");
            mDocumentHandle.setDocumentState(DocumentState.FINISHED);
            return;
        }

        mDocumentHandle.setDocumentContext(documentContext);
        mTelemetryProvider.incrementCount(mPrepareEmbeddedDocCount);
    }


    /**
     * Handle any actions (e.g extension loads) due to content refresh before handling success
     *
     * @param content
     * @param documentOptions
     * @param mediator
     * @param documentConfigHandle
     */
    private void onContentRefreshComplete(Content content,
                                          DocumentOptions documentOptions,
                                          ExtensionMediator mediator,
                                          long documentConfigHandle) {
        Log.i(TAG, "Content refresh complete");
        if (mediator != null && documentOptions != null && documentOptions.getExtensionGrantRequestCallback() != null) {
            Map<String, Object> flags = documentOptions.getExtensionFlags() != null ? documentOptions.getExtensionFlags() : new HashMap<>();
            mediator.initializeExtensions(flags, content, documentOptions.getExtensionGrantRequestCallback());

            mediator.loadExtensions(
                    flags,
                    content,
                    new ExtensionMediator.ILoadExtensionCallback() {
                        @Override
                        public Runnable onSuccess() {
                            return () -> handleEmbeddedDocumentRequestSuccess(content, documentConfigHandle);
                        }

                        @Override
                        public Runnable onFailure() {
                            return () -> mDocumentHandle.setDocumentState(DocumentState.ERROR);
                        }
                    }
            );
        } else {
            handleEmbeddedDocumentRequestSuccess(content, documentConfigHandle);
        }
    }

    @Override
    public void onDocumentStateChanged(DocumentState state, DocumentHandle handle) {
        // when the document state is prepared then content is done so call success
        if (state == DocumentState.PREPARED) {
            mHandler.post(() -> {
                if (mDocumentHandle != null) {
                    final Content content = mDocumentHandle.getContent();
                    final ExtensionMediator mediator = mDocumentHandle.getExtensionMediator();
                    long mediatorNativeHandle = mediator != null ? mediator.getNativeHandle() : 0;
                    long documentConfigHandle = nCreateDocumentConfig(mediatorNativeHandle);

                    mDocumentHandle.setDocumentConfig(new DocumentConfig(documentConfigHandle));

                    // Conditional inflation doesn't automatically work since the embedded document doesn't have
                    // a DocumentConfig on initial Content creation
                    // Manually refresh the content with DocumentConfig and proceed
                    content.refresh(mEmbeddedDocumentRequestProxy.getNativeHandle(), documentConfigHandle);
                    if (content.isWaiting()) {
                        Log.i(TAG, "Content is waiting, hence resolving the request");
                        content.resolve(new Content.CallbackV2() {
                            @Override
                            public void onComplete(Content content) {
                                onContentRefreshComplete(content, mDocumentHandle.getDocumentOptions(), mediator, documentConfigHandle);
                            }
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error occurred during content refresh: " + e.getMessage());
                            }
                        });
                    } else {
                        onContentRefreshComplete(content, mDocumentHandle.getDocumentOptions(), mediator, documentConfigHandle);
                    }

                } else {
                    mEmbeddedDocumentRequestProxy.failure("DocumentHandle is null");
                }
            });
        } else if (state == DocumentState.ERROR) {
            mHandler.post(() -> mEmbeddedDocumentRequestProxy.failure("Content creation error, Document State set to Error"));
            mTelemetryProvider.fail(mPrepareEmbeddedDocCount);
        }
    }

    private static native long nCreateDocumentConfig(long mediatorHandle);
}
