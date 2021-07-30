/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.thread.Threading;

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
        mExecutor.execute(() -> fetchInternal(source, successCallback, failureCallback));
    }

    private void fetchInternal(Uri source, SuccessCallback<Uri, T> successCallback, FailureCallback<Uri> failureCallback) {
        String scheme = source.getScheme();
        if (scheme == null) {
            failureCallback.onFailure(source, "No scheme for source: " + source);
            return;
        }
        RequestHandler<T> requestHandler = mRequestHandlers.get(scheme);
        if (requestHandler == null) {
            failureCallback.onFailure(source, "No handler registered for source: " + source);
        } else {
            requestHandler.fetch(source, successCallback, failureCallback);
        }
    }

    /**
     * Interface for a URI scheme-based RequestHandler.
     * @param <T> the type of data to fetch.
     */
    public interface RequestHandler<T> {
        /**
         * @return a list of the {@link Uri} schemes that are supported.
         */
        @NonNull List<String> supportedSchemes();

        /**
         * Request a file from the given source.
         * @param source            The source Uri. Guaranteed to have a scheme that is in this {@link RequestHandler#supportedSchemes()}.
         * @param successCallback   The callback for a successful fetch.
         * @param failureCallback   The callback for a failure to fetch.
         */
        void fetch(@NonNull Uri source, @NonNull SuccessCallback<Uri, T> successCallback, @NonNull FailureCallback<Uri> failureCallback);
    }
}
