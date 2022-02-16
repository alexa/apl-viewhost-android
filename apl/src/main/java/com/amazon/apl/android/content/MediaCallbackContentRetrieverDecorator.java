/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.dependencies.IContentRetriever;

import java.util.Collections;
import java.util.Map;

/**
 * This class wraps another IContentRetriever to notify core that media is loaded when a
 * vector graphic or other media is requested.
 */
public class MediaCallbackContentRetrieverDecorator<TResult> implements IContentRetriever<Uri, TResult> {
    private final IAPLViewPresenter mPresenter;
    private final IContentRetriever<Uri, TResult> mWrappedRetriever;

    private MediaCallbackContentRetrieverDecorator(IAPLViewPresenter presenter, IContentRetriever<Uri, TResult> wrappedRetriever) {
        this.mPresenter = presenter;
        this.mWrappedRetriever = wrappedRetriever;
    }

    public static <TResult> MediaCallbackContentRetrieverDecorator create(IAPLViewPresenter presenter, IContentRetriever<Uri, TResult> wrappedRetriever) {
        return new MediaCallbackContentRetrieverDecorator<>(presenter, wrappedRetriever);
    }

    @Override
    public void fetch(@NonNull Uri uri, @NonNull IContentRetriever.SuccessCallback<Uri, TResult> successCallback, IContentRetriever.FailureCallback<Uri> failureCallback) {
        fetchV2(uri, successCallback, (request, failMessage, errorCode) -> failureCallback.onFailure(request, failMessage));
    }

    @Override
    public void fetchV2(@NonNull Uri uri, @NonNull IContentRetriever.SuccessCallback<Uri, TResult> successCallback, IContentRetriever.FailureCallbackV2<Uri> failureCallback) {
        fetchV2(uri, Collections.emptyMap(), successCallback, failureCallback);
    }

    @Override
    public void fetchV2(@NonNull Uri uri, @NonNull Map<String, String> headers, @NonNull IContentRetriever.SuccessCallback<Uri, TResult> successCallback, IContentRetriever.FailureCallbackV2<Uri> failureCallback) {
        mWrappedRetriever.fetchV2(uri,
                headers,
                (request, result) -> {
                    mPresenter.mediaLoaded(uri.toString());
                    successCallback.onSuccess(request, result);
                },
                (request, message, errorCode) -> {
                    mPresenter.mediaLoadFailed(uri.toString(), errorCode, message);
                    failureCallback.onFailure(request, message, errorCode);
                });
    }
}
