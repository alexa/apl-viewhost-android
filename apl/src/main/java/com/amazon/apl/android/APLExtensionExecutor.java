/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.os.Handler;
import android.os.Looper;

import com.amazon.alexaext.ExtensionExecutor;

import java.lang.ref.WeakReference;

/**
 * Implementation of ExtensionExecutor which provides extension task processing
 * associated to a RootContext.
 */
public class APLExtensionExecutor extends ExtensionExecutor {
    // Maintain only a weak reference to RootContext once it's available so
    // that we don't end up with a circular reference.
    private WeakReference<RootContext> mWeakRootContext = new WeakReference<>(null);

    /**
     * Called when a root context becomes available (after inflation).
     *
     * @param rootContext  The root context (an inflated document)
     */
    public synchronized void setRootContext(RootContext rootContext) {
        mWeakRootContext = new WeakReference<>(rootContext);
    }

    /**
     * Called internally whenever a task has been added to the JNI executor's internal queue.
     */
    @Override
    protected synchronized void onTaskAdded() {
        RootContext ctx = mWeakRootContext.get();
        if (ctx != null) {
            ctx.post(this::executeTasks);
        } else {
            /*
             * When an extension launches a new task in onRegistered (for example: updating
             * live data), when the task is executed straight away without posting it,
             * the task will be processed recursively while processing the registration message,
             * and will result in a crash.  Because of this, the task needs to be scheduled to run
             * in the calling thread's next frame, or if it's not possible, on the main thread.
             */
            Looper looper = Looper.myLooper();
            if (looper == null) {
                looper = Looper.getMainLooper();
            }
            new Handler(looper).post(this::executeTasks);
        }
    }
}
