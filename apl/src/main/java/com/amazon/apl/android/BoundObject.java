/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

public abstract class BoundObject {

    /* the handle to the native object */
    private long nativeHandle = 0L;

    /**
     * Binds this object to a native peer.
     *
     * @param nativeHandle The handle to the native peer.
     */
    public final void bind(long nativeHandle) {
        this.nativeHandle = nativeHandle;
        APLBinding.register(this);
    }


    /**
     * @return The handle to the native peer.
     */
    public final long getNativeHandle() {
        return nativeHandle;
    }

    /**
     * @return True when the native peer handle is non-zero.
     */
    public final boolean isBound() {
        return (nativeHandle != 0);
    }
}