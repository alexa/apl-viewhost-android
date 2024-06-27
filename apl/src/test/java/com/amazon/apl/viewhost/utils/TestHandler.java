/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.utils;

import androidx.annotation.NonNull;

import com.amazon.apl.android.thread.IHandler;

import java.util.ArrayDeque;
import java.util.Deque;

public class TestHandler implements IHandler {
    private final Deque<Runnable> mRunnables = new ArrayDeque<>();
    @Override
    public boolean post(@NonNull Runnable r) {
        mRunnables.offer(r);
        return true;
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