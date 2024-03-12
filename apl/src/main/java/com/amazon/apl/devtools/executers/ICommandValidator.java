/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import com.amazon.apl.devtools.models.error.DTException;

public interface ICommandValidator {
    void validate() throws DTException;
}
