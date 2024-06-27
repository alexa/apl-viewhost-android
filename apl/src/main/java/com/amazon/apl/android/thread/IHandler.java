/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.thread;

import androidx.annotation.NonNull;

/**
 * An interface over the Android {@link android.os.Handler}.
 */
public interface IHandler {
    public boolean post(@NonNull Runnable r);
}
