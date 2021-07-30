/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

/**
 * Provides URI scheme validator to be used for downloading Image sources. Images will only be
 * downloaded from the schemes validated by this implementation.
 */
public interface IImageUriSchemeValidator {
    /**
     * Checks if the specified {@code scheme} is valid.
     * @param scheme the URI scheme (e.g. https, http).
     * @param aplVersion the APL version specified in the document. Refer
     * {@link com.amazon.apl.android.APLVersionCodes}.
     *
     * @return true if the scheme is valid, false otherwise.
     */
    boolean isUriSchemeValid(String scheme, int aplVersion);
}
