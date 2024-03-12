/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.request;

import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Represents a request to render an APL document from a document payload.
 */
@AutoValue
public abstract class RenderDocumentRequest {
    /**
     * The token to use for this request (optional). If provided, subsequent requests to manipulate
     * the document (e.g. ExecuteCommandsRequest) must match the token provided here. In case of a
     * mismatch, the request will be ignored. Tokens are treated as opaque values by the viewhost,
     * they are only ever compared for equality.
     */
    @Nullable
    public abstract String getToken();

    /**
     * Document payload (required). The payload can be an APL document, or an APL payload wrapped in
     * a typical Alexa Voice Service (AVS) envelope. If the document payload corresponds to an AVS
     * envelope, this envelope will be used as a fall back data source in case no data source was
     * provided with this request.
     */
    public abstract Decodable getDocument();

    /**
     * The source for data (parameters) used by the APL document (optional).
     */
    @Nullable
    public abstract Decodable getData();

    /**
     * The document session to use when rendering this document (required).
     */
    public abstract DocumentSession getDocumentSession();

    /**
     * The options to use for preparing and rendering the document (optional). These options
     * override any default options provided at the time the viewhost was created.
     */
    @Nullable
    public abstract DocumentOptions getDocumentOptions();

    public static Builder builder() { return new AutoValue_RenderDocumentRequest.Builder(); }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder token(String token);
        public abstract Builder document(Decodable document);
        public abstract Builder data(Decodable data);
        public abstract Builder documentSession(DocumentSession documentSession);
        public abstract Builder documentOptions(DocumentOptions documentOptions);
        public abstract RenderDocumentRequest build();
    }
}
