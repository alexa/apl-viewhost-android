/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.enums;

import androidx.annotation.NonNull;

public enum TargetType {
    VIEW(TargetType.VIEW_TEXT);

    private static final String VIEW_TEXT = "view";
    private final String mTargetTypeText;

    TargetType(String targetTypeText) {
        mTargetTypeText = targetTypeText;
    }

    @NonNull
    @Override
    public String toString() {
        return mTargetTypeText;
    }
}
