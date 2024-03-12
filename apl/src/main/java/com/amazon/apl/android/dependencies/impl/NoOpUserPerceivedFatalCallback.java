/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies.impl;

import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;

/**
 * Implementation of {@link IUserPerceivedFatalCallback}.
 */

public class NoOpUserPerceivedFatalCallback implements IUserPerceivedFatalCallback {
    @Override
    public void onFatalError(String error) {
        // no-op
    }

    @Override
    public void onSuccess() {
        // no-op
    }
}