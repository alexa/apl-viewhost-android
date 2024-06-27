/*
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
package com.amazon.apl.android.metrics.impl;

import androidx.annotation.NonNull;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.amazon.apl.android.thread.IHandler;

/**
 * MetricsHandler is a singleton class containing logic to post tasks as a {@link Runnable}
 * to be processed later in a background thread.
 * <p>
 * The singleton is expected to be used for all {@link com.amazon.apl.android.metrics.IMetricsRecorder}
 * implementations
 */
public class MetricsHandler implements IHandler {
    private static Handler mHandler;

    private static MetricsHandler mInstance;

    private MetricsHandler() {
        HandlerThread mThread = new HandlerThread("MetricsHandler");
        mThread.start();

        // Use the async handler if possible
        // Messages and runnables posted to an async handler are not subject
        // to synchronization barriers such as display vsync.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mHandler = Handler.createAsync(mThread.getLooper());
        } else {
            mHandler = new Handler(mThread.getLooper());
        }
    }

    public static MetricsHandler getInstance() {
        if (mInstance == null) {
            mInstance = new MetricsHandler();
        }

        return mInstance;
    }

    public boolean post(@NonNull Runnable r) {
        return mHandler.post(r);
    }
}
