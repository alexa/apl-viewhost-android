/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.request;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Represents a request issued by a runtime to finish (i.e. stop rendering) a document. When a
 * document is finished, the resources it uses are reclaimed.
 */
@AutoValue
public abstract class FinishDocumentRequest {
    /**
     * The token to use for this request (optional). If provided, the token will be compared to the
     * token specified with the document, if any. If the document's token does not match the token
     * in this request, the request will be ignored.
     */
    @Nullable
    public abstract String getToken();

    public static Builder builder() {
        return new AutoValue_FinishDocumentRequest.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder token(String token);
        public abstract FinishDocumentRequest build();
    }
}
