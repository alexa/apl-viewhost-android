/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.espresso;

import android.view.View;

import androidx.test.espresso.IdlingResource;

/**
 * Polls the view to see if a draw call has occurred recently.
 */
public class APLViewIdlingResource implements IdlingResource {
    private static final long DEFAULT_POLL_INTERVAL = 100;
    private long mLastDrawCall = 0;
    private boolean mIsIdle = true;
    private IdlingResource.ResourceCallback mResourceCallback;
    private final View mView;
    private final long mPollInterval;
    private final long mMillisIdle;

    /**
     * Constructor using defaults timings.
     * @param view the view to poll for draw calls.
     */
    public APLViewIdlingResource(View view) {
        this(view, DEFAULT_POLL_INTERVAL, 2 * DEFAULT_POLL_INTERVAL);
    }

    /**
     * Constructor with custom timings.
     * @param view          the view to poll for draw calls.
     * @param pollInterval  the interval to check in ms
     * @param millisIdle    the interval between draw calls to consider the view to be idle.
     */
    public APLViewIdlingResource(View view, long pollInterval, long millisIdle) {
        mPollInterval = pollInterval;
        mMillisIdle = millisIdle;
        mView = view;
        mView.getViewTreeObserver().addOnDrawListener(() -> {
            mIsIdle = false;
            mLastDrawCall = System.currentTimeMillis();
        });
        mView.post(this::checkIdle);
    }

    private void checkIdle() {
        long now = System.currentTimeMillis();
        boolean wasIdle = mIsIdle;
        mIsIdle = now - mLastDrawCall > mMillisIdle;
        if (mIsIdle && !wasIdle && mResourceCallback != null) {
            mResourceCallback.onTransitionToIdle();
        }
        mView.postDelayed(this::checkIdle, mPollInterval);
    }

    @Override
    public String getName() {
        return "APLViewIdlingResource";
    }

    @Override
    public boolean isIdleNow() {
        return mIsIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        mResourceCallback = callback;
    }
}