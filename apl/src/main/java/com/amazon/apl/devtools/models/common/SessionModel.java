/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

public abstract class SessionModel {
    private final String mSessionId;

    protected SessionModel(String sessionId) {
        mSessionId = sessionId;
    }

    public String getSessionId() {
        return mSessionId;
    }
}
