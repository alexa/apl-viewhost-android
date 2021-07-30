/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.thread;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides basic threading utilities.
 */
public final class Threading {

    public static final String TAG = "Threading";

    /**
     * Provides a default thread pool.
     */
    public static final ExecutorService THREAD_POOL_EXECUTOR = createExecutorService();

    private Threading() { }

    /**
     * Creates a new serial executor based upon the default thread pool.
     *
     * @return The serial executor.
     */
    public static SequentialExecutor createSequentialExecutor() {
        return new SequentialExecutor(THREAD_POOL_EXECUTOR);
    }

    public static ScheduledExecutorService createScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    private static ExecutorService createExecutorService() {
        // If possible, use the shared thread pool which is pre-optimised for the system
        // and already used by the AsyncTask mechanism.
        if (AsyncTask.THREAD_POOL_EXECUTOR instanceof ExecutorService) {
            return (ExecutorService) AsyncTask.THREAD_POOL_EXECUTOR;
        }

        Log.e(TAG, "Falling back to unoptimised thread pool");

        return Executors.newCachedThreadPool();
    }
}
