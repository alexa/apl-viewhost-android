/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import com.amazon.common.BoundObject;

public abstract class ExtensionExecutor extends BoundObject {
    public ExtensionExecutor() {
        final long handle = nCreate();
        bind(handle);
    }

    protected void executeTasks() {
        nExecuteTasks(getNativeHandle());
    }

    protected void onTaskAdded() {}

    @SuppressWarnings("unused")
    private void onTaskAddedInternal() {
        onTaskAdded();
    }

    private native long nCreate();
    private native void nExecuteTasks(long _handler);
}
