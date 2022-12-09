/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

/**
 * The clock interface that will call onTick on a set interval.
 */
public interface IClock {
    /**
     * Starts calling onTick.
     */
    void start();

    /**
     * Stops calling onTick.
     */
    void stop();

    interface IClockCallback {
        void onTick(long frameTime);
    }
}


