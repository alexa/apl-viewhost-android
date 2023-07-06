/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.example;

import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Illustration of a runtime-owned component that keeps track of embedded document requests and
 * fulfills them with incoming documents.
 */
public class ExampleDocumentFactory implements EmbeddedDocumentFactory {
    Map<String, EmbeddedDocumentRequest> mPendingRequests;
    Map<String, PreparedDocument> mPreparedDocuments;
    private final Viewhost mViewhost;

    public ExampleDocumentFactory(Viewhost viewhost) {
        mViewhost = viewhost;
        mPendingRequests = new HashMap<>();
        mPreparedDocuments = new HashMap<>();
    }

    /**
     * Called when the runtime has received a ProvideDocument directive.
     *
     * @param uri      Corresponds to the presentationUri in the ProvideDocument directive header
     *                 and it also matches the APL Host component's source property.
     * @param document The APL document (JSON-encoded string)
     */
    public void onProvideDocument(String uri, String document) {
        PrepareDocumentRequest preparedDocumentRequest =
                PrepareDocumentRequest.builder()
                        .document(new JsonStringDecodable(document))
                        .documentSession(DocumentSession.create())
                        .build();
        PreparedDocument preparedDocument = mViewhost.prepare(preparedDocumentRequest);
        synchronized (this) {
            mPreparedDocuments.put(uri, preparedDocument);
            if (mPendingRequests.containsKey(uri)) {
                mPendingRequests.get(uri).resolve(preparedDocument);
            }
        }
    }

    @Override
    public void onDocumentRequested(EmbeddedDocumentRequest request) {
        synchronized (this) {
            if (mPreparedDocuments.containsKey(request.getSource())) {
                // viewhost will handle invoking the corresponding callback to core on the core thread
                request.resolve(mPreparedDocuments.get(request.getSource()));
            } else {
                mPendingRequests.put(request.getSource(), request);
            }
        }
    }
}
