/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.edittext;

import com.amazon.common.BoundObject;

public class EditText extends BoundObject {
    public EditText(long nativeHandle) {
        bind(nativeHandle);
    }

    public void focusChanged(boolean focused) {
        nFocusChanged(getNativeHandle(), focused);
    }

    public void submit() {
        nSubmit(getNativeHandle());
    }

    public void textChanged(String text) {
        nTextChanged(getNativeHandle(), text);
    }

    private static native void nFocusChanged(long handle, boolean hasFocus);
    private static native void nSubmit(long handle);
    private static native void nTextChanged(long handle, String text);
}