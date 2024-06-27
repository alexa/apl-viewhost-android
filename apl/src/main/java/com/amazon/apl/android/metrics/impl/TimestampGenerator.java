/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics.impl;

import android.os.SystemClock;

import com.amazon.apl.android.metrics.ITimestampGenerator;

public class TimestampGenerator implements ITimestampGenerator {
    @Override
    public long generateTimeStamp() {
        return SystemClock.elapsedRealtimeNanos();
    }
}
