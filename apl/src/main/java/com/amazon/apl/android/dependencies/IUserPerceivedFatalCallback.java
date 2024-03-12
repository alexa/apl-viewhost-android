/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies;

/**
 * Callback provided by the runtime to report on the success or failure of an interaction.
 * An example of an interaction is rendering an APL document.
 * The view host guarantees that for the interaction, either `onSuccess` or `onFatalError` will be called exactly once.
 */
public interface IUserPerceivedFatalCallback {

    /**
     * To be called when user perceives UPF and report it to runtime.
     * @param error  send error. As per UPF Contract, it should be less than 100 characters.
     */
    void onFatalError(String error);

    /**
     * To be called when an interaction is successful.
     */
    void onSuccess();
}