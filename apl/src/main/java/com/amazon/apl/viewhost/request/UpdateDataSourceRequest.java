/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.request;

import androidx.annotation.NonNull;

import com.amazon.apl.viewhost.primitives.Decodable;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Represents a request to update data source.
 */
@AutoValue
public abstract class UpdateDataSourceRequest {
    /**
     * The token to use for this request (optional). If provided, the token will be compared to the
     * token specified with the document, if any. If the document's token does not match the token
     * in this request, the request will be ignored.
     */
    @Nullable
    public abstract String getToken();
    /**
     * Get the update type such as dynamicIndexList, dynamicTokenList.
     * @return
     */
    @Nullable
    public abstract String getType();
    /**
     * Payload to update data source.
     */
    @NonNull
    public abstract Decodable getData();

    /**
     * Callback to be notified about the result of executing these commands
     */
    @Nullable
    public abstract UpdateDataSourceCallback getCallback();

    public static UpdateDataSourceRequest.Builder builder() {
        return new AutoValue_UpdateDataSourceRequest.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder token(String token);
        public abstract Builder type(String type);
        public abstract Builder data(Decodable data);
        public abstract Builder callback(UpdateDataSourceCallback callback);
        public abstract UpdateDataSourceRequest build();
    }

    /**
     * Interface for informing the caller about the result of this updateDataSource request
     */
    public interface UpdateDataSourceCallback {
        /**
         * Called when the DataSource update have been applied.
         */
        void onSuccess();

        /**
         * Called when the DataSource update could not be applied.
         */
        void onFailure(String reason);
    }
}
