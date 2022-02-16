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
 * If we are attached to a RootContext, post the task to it.
 * If not, try to schedule the task on calling thread; if this is not possible,
 * schedule on main thread.
 */
public class APLExtensionExecutor extends ExtensionExecutor {
    WeakReference<RootContext> mWeakRootContext = new WeakReference<>(null);

    public APLExtensionExecutor() {
        super();
    }

    public void setRootContext(RootContext rootContext) {
        mWeakRootContext = new WeakReference<>(rootContext);
    }


    @Override
    protected void onTaskAdded() {
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
