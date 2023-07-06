/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * Special executor for testing
 */
public class ManualExecutor implements Executor {
    private final Deque<Runnable> mRunnables = new ArrayDeque<>();

    @Override
    public synchronized void execute(final Runnable runnable) {
        mRunnables.offer(runnable);
    }

    public int size() {
        return mRunnables.size();
    }

    public void flush() {
        Runnable runnable = mRunnables.poll();
        while (runnable != null) {
            runnable.run();
            runnable = mRunnables.poll();
        }
    }

    public boolean isEmpty() {
        return mRunnables.isEmpty();
    }
}
