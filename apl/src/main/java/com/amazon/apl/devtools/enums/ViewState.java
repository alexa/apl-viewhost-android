/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.enums;

import androidx.annotation.NonNull;

public enum ViewState {
    // Initial state
    EMPTY(ViewState.EMPTY_TEXT),
    // When a target is attached or a view is displayed on the screen
    INFLATED(ViewState.INFLATED_TEXT),
    // When the view has been assigned a document
    LOADED(ViewState.LOADED_TEXT),
    // When the document is finished rendering and loading packages
    READY(ViewState.READY_TEXT),
    // When a view has some catastrophic error that prevents display, such as an invalid document
    FAILED(ViewState.FAILED_TEXT);

    private static final String EMPTY_TEXT = "empty";
    private static final String INFLATED_TEXT = "inflated";
    private static final String LOADED_TEXT = "loaded";
    private static final String READY_TEXT = "ready";
    private static final String FAILED_TEXT = "failed";
    private final String mViewStateText;

    ViewState(String viewStateText) {
        mViewStateText = viewStateText;
    }

    @NonNull
    @Override
    public String toString() {
        return mViewStateText;
    }
}
