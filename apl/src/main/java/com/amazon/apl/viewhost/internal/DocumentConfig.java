/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import com.amazon.common.BoundObject;

/**
 * Peer class for documentConfig class in core.
 */
public class DocumentConfig extends BoundObject {
    public DocumentConfig(long nativeHandle) {
        bind(nativeHandle);
    }
}
