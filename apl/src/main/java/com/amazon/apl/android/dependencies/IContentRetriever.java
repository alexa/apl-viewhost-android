/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import androidx.annotation.NonNull;

/**
 * Interface for a content retriever.
 * @param <S> the object request type
 * @param <T> the type of content to retrieve
 */
public interface IContentRetriever<S,T> {
    /**
     * Fetch content for the following request.
     * @param request           the request
     * @param successCallback   the callback for a successful result
     * @param failureCallback   the callback for a failed result
     */
    void fetch(@NonNull S request, @NonNull SuccessCallback<S, T> successCallback, @NonNull FailureCallback<S> failureCallback);

    interface SuccessCallback<S, T> {
        void onSuccess(@NonNull S request, @NonNull T content);
    }

    interface FailureCallback<S> {
        void onFailure(@NonNull S request, @NonNull String failMessage);
    }
}
