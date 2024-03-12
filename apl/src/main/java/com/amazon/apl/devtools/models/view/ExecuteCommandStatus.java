/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;


import androidx.annotation.NonNull;

public enum ExecuteCommandStatus {
    COMPLETED("completed"),
    TERMINATED("terminated");

    private final String mStatus;

    ExecuteCommandStatus(String status) {
        mStatus = status;
    }

    @Override
    @NonNull
    public String toString() {
        return mStatus;
    }
}
