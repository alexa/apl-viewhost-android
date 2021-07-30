/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

public class ExtensionEventHandler extends BoundObject {

    public ExtensionEventHandler(String uri, String name) {
        long handle = nCreate(uri, name);
        if (handle != 0) {
            bind(handle);
        }
    }


    /**
     * @return The extension URI associated with this event handler
     */
    public String getURI() {
        return nGetURI(getNativeHandle());
    }


    /**
     * @return The name of this event handler
     */
    public String getName() {
        return nGetName(getNativeHandle());
    }

    private static native long nCreate(String uri, String name);

    private static native String nGetURI(long nativeHandle);

    private static native String nGetName(long nativeHandle);
}
