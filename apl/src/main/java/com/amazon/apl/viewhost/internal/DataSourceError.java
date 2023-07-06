/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class is a peer for DocumentError stuct in core. It defines errors corresponding to a document.
 */
@AllArgsConstructor
@Getter
public class DataSourceError {
    private long documentContextId;
    private Object error;
    private boolean isTopDocument;
}
