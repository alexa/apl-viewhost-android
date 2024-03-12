/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.enums;

public enum DTError {
    // Command request is in a valid format, but the method specified has not been implemented
    METHOD_NOT_IMPLEMENTED(1, "Method Not Implemented."),

    INVALID_SESSION_ID(2, "Invalid Session Id."),

    NO_PERFORMANCE_METRICS(3, "No Performance Metrics."),

    TARGET_ALREADY_ATTACHED(100, "Target Already Attached."),

    TARGET_NOT_ATTACHED(101, "Target Not Attached."),

    NO_SUCH_TARGET(102, "No Such Target."),

    LOG_ALREADY_ENABLED(200, "Log Already Enabled."),

    LOG_ALREADY_DISABLED(201, "Log Already Disabled."),

    // Invalid document provided in a command request, such as in setDocument and executeCommands
    INVALID_DOCUMENT(300, "Invalid Document."),

    // Invalid command request format, usually caught and thrown from a JSONException
    INVALID_COMMAND(301, "Invalid Command."),

    NETWORK_ALREADY_ENABLED(400, "Network Already Enabled."),

    NETWORK_ALREADY_DISABLED(401, "Network Already Disabled."),

    PERFORMANCE_ALREADY_ENABLED(500, "Performance Already Enabled."),

    PERFORMANCE_ALREADY_DISABLED(501, "Performance Already Disabled."),

    NO_DOCUMENT_RENDERED(502, "No Document Rendered."),
    UNKNOWN_ERROR(503, "Unknown Error."),
    METHOD_FAILURE(504, "Method Execution Failed.");

    private final int mErrorCode;
    private final String mErrorMsg;

    DTError(int code, String errorMsg) {
        mErrorCode = code;
        mErrorMsg = errorMsg;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public String getErrorMsg() {
        return mErrorMsg;
    }
}
