/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.sgcontent;

import com.amazon.apl.android.scenegraph.edittext.EditText;
import com.amazon.apl.android.scenegraph.edittext.EditTextConfig;

public class EditTextNode extends Node {
    public EditTextNode(long address) {
        super(address);
    }

    public EditText getEditText() {
        return nGetEditText(mAddress);
    }

    public EditTextConfig getEditTextConfig() {
        return new EditTextConfig(nGetTextConfig(mAddress));
    }

    public String getText() {
        return nGetText(mAddress);
    }

    private static native EditText nGetEditText(long address);
    private static native long nGetTextConfig(long address);
    private static native String nGetText(long address);
    private static native float[] nGetSize(long address);
}