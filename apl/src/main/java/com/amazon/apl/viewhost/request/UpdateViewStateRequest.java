/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.request;

import com.amazon.apl.viewhost.primitives.DisplayState;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Represents a request to modify the view state
 */
@AutoValue
public abstract class UpdateViewStateRequest {
    /**
     * Specifies the view's new display state.
     * - null: The display state should not be changed with this request.
     */
    @Nullable
    public abstract DisplayState getDisplayState();

    /**
     * The desired maximum frame rate, measured in cycles per second (Hz). Can be used to reduce the
     * rate of processing for runtime-controlled performance optimization.
     * - A negative value means to use the default frame rate for the device (unthrottled).
     * - A value of 0 means that the frame loop is stopped. Any incoming execute commands, dynamic
     *   data updates, extension messages will queued. This has the additional behavior of stopping
     *   elapsed time until a non-zero frame rate is specified. This usage is not normally
     *   recommended as it may produce an additional jank when processing is resumed.
     * - A positive value indicates an intention to operate at throttled (reduced) processing rate.
     *   This only has an impact if the value specified is less than the actual frame rate of the
     *   device. It cannot be used to increase the rate beyond the device's normal rate.
     * - null: The frame rate should not be changed with this request.
     */
    @Nullable
    public abstract Double getProcessingRate();

    /**
     * Callback to inform the caller when the request has been processed. If a display state change
     * is requested, an active document will be given the opportunity to trigger events (e.g.
     * SendEvent) before this callback is triggered.
     */
    @Nullable
    public abstract UpdateViewStateCallback getCallback();

    public static Builder builder() {
        return new AutoValue_UpdateViewStateRequest.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder displayState(DisplayState displayState);
        public abstract Builder processingRate(Double procesingRate);
        public abstract Builder callback(UpdateViewStateCallback callback);
        public abstract UpdateViewStateRequest build();
    }

    /**
     * Interface for informing the caller when the request has been processed
     */
     public interface UpdateViewStateCallback {
        /**
         * Called when the view state update request has been applied.
         */
         void onComplete();
     }
}
