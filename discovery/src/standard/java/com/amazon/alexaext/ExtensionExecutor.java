/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import com.amazon.common.BoundObject;

/**
 * A peer class to the JNI executor. It receives a notification (onTaskAddedInternal) whenever an
 * extension task is added to a queue. Implementations are expected to override onTaskAdded() and
 * call executeTasks() when possible on a core-safe thread.
 */
public abstract class ExtensionExecutor extends BoundObject {
    // As a workaround for threading problems related to async inflation there is a pause/resume
    // mechanism in this executor class. The executor should be paused during async inflation to
    // prevent concurrent access to core objects.
    protected boolean mIsPaused = false;

    public ExtensionExecutor() {
        final long handle = nCreate();
        bind(handle);
    }

    /**
     * Pauses the executor.
     *
     * This stops internal notification of tasks. Tasks will accumulate in JNI layer. 
     */
    public synchronized void pause() {
        mIsPaused = true;
    }

    /**
     * Resumes the executor.
     *
     * This resumes internal notification of tasks. This also generates a notification to ensure the
     * JNI queue is flushed.
     */
    public synchronized void resume() {
        mIsPaused = false;
        onTaskAdded();
    }

    /**
     * Called by an implementation to execute tasks that have been queued within the JNI layer. This
     * must be called on the core thread to ensure that RootContext is not concurrently modified by
     * multiple threads.
     */
    protected void executeTasks() {
        nExecuteTasks(getNativeHandle());
    }

    /**
     * Called internally whenever a task has been added to the JNI executor's internal queue.
     *
     * Implementation is expected to:
     * - Guarantee an eventual call to executeTasks() upon being notified of new tasks, otherwise
     *   the internal queue will grow without bounds.
     * - Ensure that when executeTasks() is called, it's called on the core thread, to prevent
     *   crashes due to concurrent access to core.
     * - Post tasks (asynchronous processing) rather than process immediately, to prevent crashes
     *   related to recursion (e.g. an extension sending live data updates in onRegistered).
     */
    protected void onTaskAdded() {}

    /**
     * Called from JNI whenever a new task has been added to the JNI executor's internal queue.
     */
    @SuppressWarnings("unused")
    private synchronized void onTaskAddedInternal() {
        if (mIsPaused) {
            // Avoid notifying executor about new tasks while paused. We'll synthesize a
            // notification when the executor is resumed.
            return;
        }
        onTaskAdded();
    }

    private native long nCreate();
    private native void nExecuteTasks(long _handler);
}
