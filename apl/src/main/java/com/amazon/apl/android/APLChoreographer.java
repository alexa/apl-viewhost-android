/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.view.Choreographer;

import androidx.annotation.UiThread;

/**
 * This is an implementation of IClock that is powered by the Android Choreographer.
 * It should run at 60 frames per second.
 */
public class APLChoreographer implements IClock, Choreographer.FrameCallback  {
    
    final IClockCallback callback;
    Boolean running = false;
    Boolean scheduled = false;

    public APLChoreographer(IClockCallback callback) {
        this.callback = callback;
    }

    @Override
    @UiThread
    public void start() {
        if (!running) {
            running = true;
            scheduled = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    @UiThread
    public void stop() {
        running = false;

        if (scheduled) {
            //Optimistically try and prevent the doFrame from happening.
            Choreographer.getInstance().removeFrameCallback(this);
            scheduled = false;
        }
    }

    @Override
    public void doFrame(long frameTime) {
        scheduled = false;
        callback.onTick(frameTime);

        if (running) {
            Choreographer.getInstance().postFrameCallback(this);
            scheduled = true;
        }
    }
}
