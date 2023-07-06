/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.config;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.DocumentSession;

import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;

import org.json.JSONObject;

/**
 * In case data is supposed to be retrieved from http, we use this class.
 * Format of fetching data:
 * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-interface.html#renderdocument-directive
 * or
 * Plain APL document.
 */
public class DefaultEmbeddedDocumentFactory implements EmbeddedDocumentFactory {
    private static final String TAG = "DefaultEmbeddedDocumentFactory";
    public static final String FIELD_DOCUMENT = "document";
    public static final String FIELD_DATASOURCES = "datasources";
    private static final String FIELD_EMPTY_FALLBACK_VALUE = "";
    private final IDataRetriever mDataRetriever;
    private final Viewhost mViewHost;

    public DefaultEmbeddedDocumentFactory(Viewhost viewhost, IDataRetriever dataRetriever) {
        mDataRetriever = dataRetriever;
        mViewHost = viewhost;
    }

    @Override
    public void onDocumentRequested(EmbeddedDocumentRequest request) {
        Log.i(TAG, "onDocumentRequested in DefaultEmbeddedDocumentFactory");
        String url = request.getSource();
        if (!isValid(url)) {
            request.fail("Invalid Url");
            return;
        }

        mDataRetriever.fetch(url, new IDataRetriever.Callback() {

            @Override
            public void success(String response) {
                try {
                    JSONObject message = new JSONObject(response);
                    String document ,data = FIELD_EMPTY_FALLBACK_VALUE;
                    if (message.has("name") && "RenderDocument".equals(message.getString("name"))) {
                        JSONObject payload = message.getJSONObject("payload");
                        document = payload.optString(FIELD_DOCUMENT, FIELD_EMPTY_FALLBACK_VALUE);
                        data = payload.optString(FIELD_DATASOURCES, FIELD_EMPTY_FALLBACK_VALUE);
                    } else {
                        document = message.toString();
                    }

                    PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                            .document(new JsonStringDecodable(document))
                            .data(new JsonStringDecodable(data))
                            .documentSession(DocumentSession.create())
                            .build();

                    PreparedDocument preparedDocument = mViewHost.prepare(prepareDocumentRequest);
                    request.resolve(preparedDocument);

                } catch (Exception ex) {
                    request.fail(String.format("failed to extract document and data because of %s", ex.getMessage()));
                }
            }

            @Override
            public void error() {
                request.fail("document could not be loaded");
            }
        });
    }

    private boolean isValid(@NonNull String url) {
        return url.startsWith("http:") || url.startsWith("https:");
    }
}

