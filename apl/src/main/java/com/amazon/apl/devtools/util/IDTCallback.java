/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

public interface IDTCallback<T> {
    void execute(T t, RequestStatus requestStatus);

    default void execute(RequestStatus requestStatus) {
        execute(null, requestStatus);
    }
}
