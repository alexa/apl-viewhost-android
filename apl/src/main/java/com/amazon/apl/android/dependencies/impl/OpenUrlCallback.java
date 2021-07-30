/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import com.amazon.apl.android.dependencies.IOpenUrlCallback;

import java.lang.ref.WeakReference;

/**
 * Default implementation of OpenUrlCallback. Opens a link into an external view.
 */
public class OpenUrlCallback implements IOpenUrlCallback {

    private static final String TAG = "OpenUrlCallback";
    @NonNull
    private final WeakReference<Context> mContext;

    /**
     * Constructor.
     * @param context The Android context.
     */
    public OpenUrlCallback(@NonNull final Context context) {
        mContext = new WeakReference<>(context);
    }

    /**
     * Opens a link through an Android intent.
     * @param url The url to open
     * @param openUrlCallbackResult Callback to notify if the open url has succeed.
     */
    @Override
    public void onOpenUrl(@NonNull String url, @NonNull IOpenUrlCallbackResult openUrlCallbackResult) {

        boolean success = false;

        // TODO -> handle non https case better
        if (url.indexOf("https://") == 0) {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (mContext.get() != null) {
                mContext.get().startActivity(intent);
                success = true;
            }
        }

        openUrlCallbackResult.onResult(success);
    }
}
