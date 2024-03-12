/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.Target;
import com.amazon.apl.devtools.models.error.DTException;

public final class CommandRequestValidator {
    private static final String TAG = CommandRequestValidator.class.getSimpleName();
    private final TargetCatalog mTargetCatalog;

    public CommandRequestValidator(TargetCatalog targetCatalog) {
        mTargetCatalog = targetCatalog;
    }

    public void validateBeforeGettingTargetFromTargetCatalog(int id, String targetId)
            throws DTException {
        Log.i(TAG, "Validating target id " + targetId);
        if (!mTargetCatalog.has(targetId)) {
            throw new DTException(id, DTError.NO_SUCH_TARGET.getErrorCode(), "No such target");
        }
    }

    public void validateBeforeCreatingSession(int id, DTConnection connection, Target target)
            throws DTException {
        Log.i(TAG, "Validating that target with target id " + target.getTargetId() +
                " is not yet attached to a session belonging to this connection");
        for (String registeredSessionId : target.getRegisteredSessionIds()) {
            if (connection.hasSession(registeredSessionId)) {
                throw new DTException(id, DTError.TARGET_ALREADY_ATTACHED.getErrorCode(),
                        "Target is already attached");
            }
        }
    }

    public void validateBeforeGettingSession(int id, String sessionId, DTConnection connection)
            throws DTException {
        Log.i(TAG, "Validating session id " + sessionId);
        if (!connection.hasSession(sessionId)) {
            throw new DTException(id, DTError.INVALID_SESSION_ID.getErrorCode(),
                    "Invalid session id");
        }
    }

    public void validatePerformanceEnabled(int id, String sessionId, Boolean isEnabled)
            throws DTException {
        Log.i(TAG, "Validating if performance metric is enabled " + sessionId);
        if (!isEnabled) {
            throw new DTException(id, DTError.PERFORMANCE_ALREADY_DISABLED.getErrorCode(),
                    "Metric is not enabled");
        }
    }

    public void validateLogEnabled(int id, String sessionId, boolean isEnabled) throws DTException {
        Log.i(TAG, "Validating if Log is enabled " + sessionId);
        if (!isEnabled) {
            throw new DTException(id, DTError.LOG_ALREADY_DISABLED.getErrorCode(),
                    "Log is not enabled");
        }
    }

    public void validateNetworkEnabled(int id, String sessionId, boolean isEnabled) throws DTException {
        Log.i(TAG, "Validating if Network is enabled " + sessionId);
        if (!isEnabled) {
            throw new DTException(id, DTError.NETWORK_ALREADY_DISABLED.getErrorCode(),
                    "Network is not enabled");
        }
    }
}
