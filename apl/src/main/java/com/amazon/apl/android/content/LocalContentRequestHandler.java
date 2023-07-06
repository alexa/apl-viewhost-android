/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Supports retrieving String assets from a file stored on the device. Uses {@link ContentResolver}.
 */
public class LocalContentRequestHandler<T> implements ContentRetriever.RequestHandler<T> {
    private final ContentResolver mContentResolver;

    /**
     * Constructs a LocalContentRequestHandler.
     * @param contentResolver The content resolver.
     */
    public LocalContentRequestHandler(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    @Override
    @NonNull
    public List<String> supportedSchemes() {
        return Arrays.asList("content", "file");
    }

    @Override
    public void fetch(@NonNull Uri source, @NonNull IContentRetriever.SuccessCallback<Uri, T> successCallback, @NonNull IContentRetriever.FailureCallback<Uri> failureCallback) {
        try (InputStream inputStream = mContentResolver.openInputStream(source)) {
            successCallback.onSuccess(source, getResponse(inputStream, source));
        } catch (IOException e) {
            failureCallback.onFailure(source, e.getMessage());
        }
    }

    public <T> T getResponse(final InputStream inputStream, final Uri source) throws IOException {
        return (T) FileUtils.readString(source.getPath(), inputStream);
    }
}
