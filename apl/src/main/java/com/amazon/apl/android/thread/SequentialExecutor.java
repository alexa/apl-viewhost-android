/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.thread;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Allows a series of async tasks to be executed in order on an arbitrary {@link Executor},
 * and for the result of each task to be monitored via a {@link Future}. A task is guaranteed to
 * complete before the next task in the sequence starts.
 *
 * This allows existing thread pools to use for sequenced tasks where the thread
 * used does not have to be consistent, but the execution order is significant, and a
 * task cannot start before the previous task in the sequence has completed.
 *
 * The {@link SequentialExecutor} is a variation of the SerialExecutor pattern, as shown here in
 * the Android Open Source Project {@link android.os.AsyncTask}:
 *
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/AsyncTask.java#281
 *
 * For more information about serial executors, see the Java manual:
 *
 * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html
 */
public class SequentialExecutor implements Executor {

    private final Executor mExecutor;
    private final Deque<FutureTask> mTaskQueue = new ArrayDeque<>();
    private Runnable mActiveTask;

    /**
     * Constructs an instance of the {@link SequentialExecutor}.
     *
     * @param executor  The underlying executor to use for processing the sequential request.
     */
    @SuppressWarnings("WeakerAccess")
    public SequentialExecutor(@NonNull final Executor executor) {
        mExecutor = Objects.requireNonNull(executor);
    }

    @Override
    public synchronized void execute(final Runnable command) {
        if(command == null) {
            return;
        }

        submit(() -> {
            command.run();

            return true;
        });
    }

    /**
     * Allows a task to be added to the execution with a corresponding {@link Future} returned.
     *
     * @param callable  The task to be executed.
     * @param <V>       The return type of the callable.
     * @return          A {@link Future} of the callable return type.
     */
    public synchronized <V> Future<V> submit(@NonNull final Callable<V> callable) {
        Objects.requireNonNull(callable);

        final FutureTask<V> task = new FutureTask<>(() -> {
            try {
                return callable.call();
            } finally {
                next();
            }
        });

        mTaskQueue.offer(task);

        if (mActiveTask == null) {
            next();
        }
        return task;
    }

    private synchronized void next() {
        mActiveTask = mTaskQueue.poll();

        if (mActiveTask != null) {
            mExecutor.execute(mActiveTask);
        }
    }

    public void clearTaskQueue(){
        mTaskQueue.clear();
    }
}
