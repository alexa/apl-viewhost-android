/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

/**
 * A timer is just a helper object that records a StartSegment event when the timer is started and a EndSegment event when the timer is stopped.
 * The start and stop segments can be related together using the relatedId property.
 */
public interface ITimer {
    // Stops the timer and emits a EndSegment event. After stopping, the timer cannot be restarted.
    void stop();

    // Stops the timer and emits a FailSegment event. After failing, the timer cannot be restarted.
    void fail();
}