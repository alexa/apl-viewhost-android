/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import androidx.annotation.NonNull;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;

/**
 * The default URI scheme validator for the Image source.
 */
public class DefaultUriSchemeValidator implements IImageUriSchemeValidator {

    /**
     * {@inheritDoc}. By default, only {@code https, content, file} are supported for
     * APL version 1.2 and higher.
     */
    @Override
    public boolean isUriSchemeValid(@NonNull String scheme, int aplVersion) {
        return aplVersion <= APLVersionCodes.APL_1_1 || "https".equals(scheme) || "data".equals(scheme) || "content".equals(scheme) || "file".equals(scheme);
    }
}
