/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Interface for a content retriever.
 * @param <S> the object request type
 * @param <T> the type of content to retrieve
 */
public interface IContentRetriever<S,T> {
    int DEFAULT_ERROR_CODE = 0;

    /**
     * Fetch content for the following request.
     * @param request           the request
     * @param successCallback   the callback for a successful result
     * @param failureCallback   the callback for a failed result
     */
    void fetch(@NonNull S request, @NonNull SuccessCallback<S, T> successCallback, @NonNull FailureCallback<S> failureCallback);

    /**
     * Fetch content for the following request, version 2 provides an additional errorCode
     * parameter in the failure callback.
     * The default implementation calls fetch with a default error code of 0 for backwards
     * compability.
     *
     * @param request           the request
     * @param successCallback   the callback for a successful result
     * @param failureCallback   the callback for a failed result
     */
    default void fetchV2(@NonNull S request, @NonNull SuccessCallback<S, T> successCallback, @NonNull FailureCallbackV2<S> failureCallback) {
        fetch(request, successCallback, (requestCallback, errorMessage) -> failureCallback.onFailure(requestCallback, errorMessage, DEFAULT_ERROR_CODE));
    }

    /**
     * Fetch content for the following request, version 2 provides an additional errorCode
     * parameter in the failure callback.
     * The default implementation calls fetch with a default error code of 0 for backwards
     * compability.
     *
     * @param request           the request
     * @param headers           A map of the headers to include in the request.
     * @param successCallback   the callback for a successful result
     * @param failureCallback   the callback for a failed result
     */
    default void fetchV2(@NonNull S request, @NonNull Map<String, String> headers, @NonNull SuccessCallback<S, T> successCallback, @NonNull FailureCallbackV2<S> failureCallback) {
        fetchV2(request, successCallback, failureCallback);
    }

    interface SuccessCallback<S, T> {
        void onSuccess(@NonNull S request, @NonNull T content);
    }

    interface FailureCallback<S> {
        void onFailure(@NonNull S request, @NonNull String failMessage);
    }

    /**
     * Version 2 interface for failure callback which provides an errorCode which will
     * be passed on to onFail callbacks.
     * @param <S>
     */
    interface FailureCallbackV2<S> {
        void onFailure(@NonNull S request, @NonNull String failMessage, @NonNull int errorCode);
    }
}
