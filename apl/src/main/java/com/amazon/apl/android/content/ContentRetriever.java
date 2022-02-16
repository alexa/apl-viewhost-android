/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.thread.Threading;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * A {@link Uri} based Content retriever that supports different schemes.
 *
 * Register the retriever in {@link com.amazon.apl.android.APLOptions} and add {@link RequestHandler}'s
 * for each scheme you support.
 *
 * @param <T> the type of content to retrieve.
 */
public class ContentRetriever<T> implements IContentRetriever<Uri, T> {
    private final Map<String, RequestHandler<T>> mRequestHandlers = new ConcurrentHashMap<>();
    private final Executor mExecutor;

    /**
     * Create a ContentRetriever with an Executor.
     * @param executor  the Executor to use.
     */
    public ContentRetriever(Executor executor) {
        mExecutor = executor;
    }

    /**
     * Create a ContentRetriever with the default Executor defined in {@link Threading}.
     */
    public ContentRetriever() {
        this(Threading.THREAD_POOL_EXECUTOR);
    }

    public ContentRetriever<T> addRequestHandler(RequestHandler<T> handler) {
        for (String scheme : handler.supportedSchemes()) {
            mRequestHandlers.put(scheme, handler);
        }
        return this;
    }

    @Override
    public void fetch(@NonNull Uri source, @NonNull SuccessCallback<Uri, T> successCallback, @NonNull FailureCallback<Uri> failureCallback) {
        mExecutor.execute(() -> fetchInternal(source, Collections.emptyMap(), successCallback, (request, errorMessage, errorCode) -> failureCallback.onFailure(request, errorMessage)));
    }

    @Override
    public void fetchV2(@NonNull Uri source, @NonNull SuccessCallback<Uri, T> successCallback, @NonNull FailureCallbackV2<Uri> failureCallback) {
        mExecutor.execute(() -> fetchInternal(source, Collections.emptyMap(), successCallback, failureCallback));
    }

    @Override
    public void fetchV2(@NonNull Uri source, @NonNull Map<String, String> headers, @NonNull SuccessCallback<Uri, T> successCallback, @NonNull FailureCallbackV2<Uri> failureCallback) {
        mExecutor.execute(() -> fetchInternal(source, headers, successCallback, failureCallback));
    }

    private void fetchInternal(Uri source, Map<String, String> headers, SuccessCallback<Uri, T> successCallback, FailureCallbackV2<Uri> failureCallbackV2) {
        String scheme = source.getScheme();
        if (scheme == null) {
            failureCallbackV2.onFailure(source, "No scheme for source", DEFAULT_ERROR_CODE);
            return;
        }
        RequestHandler<T> requestHandler = mRequestHandlers.get(scheme);
        if (requestHandler == null) {
            failureCallbackV2.onFailure(source, "No handler registered for source", DEFAULT_ERROR_CODE);
        } else {
            requestHandler.fetchV2(source, headers, successCallback, failureCallbackV2);
        }
    }

    /**
     * Interface for a URI scheme-based RequestHandler.
     * @param <T> the type of data to fetch.
     */
    public interface RequestHandler<T> extends IContentRetriever<Uri, T> {
        /**
         * @return a list of the {@link Uri} schemes that are supported.
         */
        @NonNull List<String> supportedSchemes();
    }
}
