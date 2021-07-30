/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

/**
 * Supports retrieving assets from an application's assets/ folder.
 */
public class LocalAssetRequestHandler implements ContentRetriever.RequestHandler<String> {
    private final WeakReference<Context> mContextRef;

    public LocalAssetRequestHandler(Context context) {
        mContextRef = new WeakReference<>(context);
    }

    @Override
    @NonNull
    public List<String> supportedSchemes() {
        return Arrays.asList("android_asset", "");
    }

    @Override
    public void fetch(@NonNull Uri uri, @NonNull IContentRetriever.SuccessCallback<Uri, String> successCallback, @NonNull IContentRetriever.FailureCallback<Uri> failureCallback) {
        // android_asset://com.amazon.apl.android.app/docs/localDoc.json
        final Context context = mContextRef.get();
        if (context == null) {
            failureCallback.onFailure(uri, "Context is null, cannot fetch: " + uri);
        }

        try {
            String authority = uri.getAuthority();
            Resources resources;
            if (TextUtils.isEmpty(authority)) {
                resources = context.getResources();
            } else {
                resources = context.getPackageManager().getResourcesForApplication(authority);
            }
            try (InputStream inputStream = resources.getAssets().open(uri.getPath().substring(1))) {
                String result = FileUtils.readString(uri.getPath(), inputStream);
                successCallback.onSuccess(uri, result);
            } catch (IOException e) {
                failureCallback.onFailure(uri, "Unable to open file: " + uri + ". " + e.getMessage());
            }
        } catch (PackageManager.NameNotFoundException e) {
            failureCallback.onFailure(uri, "Package name not found for: " + uri + ". " + e.getMessage());
        }
    }
}
