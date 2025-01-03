/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import com.amazon.common.BoundObject;

public class PackageRequest extends BoundObject {
    public PackageRequest(long nativeHandle) {
        bind(nativeHandle);
    }
}
