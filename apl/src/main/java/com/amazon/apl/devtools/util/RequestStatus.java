/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import com.amazon.apl.devtools.enums.DTError;

/**
 * This class will track the execution status of any given request.
 */
public class RequestStatus {
    private final ExecutionStatus mStatus;
    private final int mId;
    private final DTError mError;

    public enum ExecutionStatus {
        SUCCESSFUL,
        FAILED
    }

    private RequestStatus(ExecutionStatus status) {
        this(status, 0, DTError.UNKNOWN_ERROR);
    }

    private RequestStatus(ExecutionStatus status, int id, DTError error) {
        mStatus = status;
        mId = id;
        mError = error;
    }

    public static RequestStatus successful() {
        return new RequestStatus(ExecutionStatus.SUCCESSFUL);
    }

    public static RequestStatus failed(int id, DTError error) {
        return new RequestStatus(ExecutionStatus.FAILED, id, error);
    }

    public ExecutionStatus getExecutionStatus() {
        return mStatus;
    }

    public int getId() {
        return mId;
    }

    public DTError getError() {
        return mError;
    }
}
