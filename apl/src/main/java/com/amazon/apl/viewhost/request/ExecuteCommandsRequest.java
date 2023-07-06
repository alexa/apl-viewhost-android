/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.request;

import com.amazon.apl.viewhost.primitives.Decodable;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Represents a request to execute externally-provided commands in the context of an APL document.
 */
@AutoValue
public abstract class ExecuteCommandsRequest {
    /**
     * The token to use for this request (optional). If provided, the token will be compared to the
     * token specified with the document, if any. If the document's token does not match the token
     * in this request, the request will be ignored.
     */
    @Nullable
    public abstract String getToken();

    /**
     * The command payload for this request (required).
     */
    public abstract Decodable getCommands();

    /**
     * Callback to be notified about the result of executing these commands
     */
    @Nullable
    public abstract ExecuteCommandsCallback getCallback();

    public static Builder builder() {
        return new AutoValue_ExecuteCommandsRequest.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder token(String token);
        public abstract Builder commands(Decodable commands);
        public abstract Builder callback(ExecuteCommandsCallback callback);
        public abstract ExecuteCommandsRequest build();
    }

    /**
     * Interface for informing the caller about the result of this execute commands request
     */
    public interface ExecuteCommandsCallback {
        /**
         * Called when the command(s) were successfully executed.
         */
        void onComplete();

        /**
         * Called when the command(s) were terminated before completion.
         */
        void onTerminated();
    }
}
