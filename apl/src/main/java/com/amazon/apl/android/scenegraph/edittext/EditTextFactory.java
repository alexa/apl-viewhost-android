/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.edittext;

import com.amazon.common.BoundObject;

public class EditTextFactory extends BoundObject {

    public EditTextFactory() {
        long handle = nCreate();
        if (handle != 0) {
            bind(handle);
        }
    }

    private EditText createEditText(long nativeHandle) {
        return new EditText(nativeHandle);
    }

    private native long nCreate();
}