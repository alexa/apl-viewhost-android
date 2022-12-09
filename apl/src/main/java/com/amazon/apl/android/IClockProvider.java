/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

/**
 * Used to provide a clock for a RootContext to use to run the APL engine with.
 */
public interface IClockProvider {
    /**
     * Provider function that should create a clock.
     * Usually called by RootContext during initialization.
     * @param callback The interface that the Clock should tick against.
     */
    IClock create(IClock.IClockCallback callback);
}
