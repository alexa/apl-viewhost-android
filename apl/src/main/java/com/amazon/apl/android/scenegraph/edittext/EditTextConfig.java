/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.edittext;

import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scenegraph.text.APLTextProperties;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.android.utils.JNIUtils;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.SubmitKeyType;

import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;

public class EditTextConfig {
    private final long mAddress;

    public EditTextConfig(long address) {
        mAddress = address;
    }

    public int getHighlightColor() {
        return ColorUtils.toARGB(nGetHighlightColor(mAddress));
    }

    public int getTextColor() {
        return ColorUtils.toARGB(nGetTextColor(mAddress));
    }

    public KeyboardType getKeyboardType() {
        return KeyboardType.valueOf(nGetKeyboardType(mAddress));
    }

    public SubmitKeyType getSubmitKeyType() {
        return SubmitKeyType.valueOf(nGetSubmitKeyType(mAddress));
    }

    public String strip(String text) {
        return JNIUtils.safeStringValues(nStrip((text + "\0").getBytes(StandardCharsets.UTF_8), mAddress));
    }

    public boolean isSecureInput() {
        return nIsSecureInput(mAddress);
    }

    public boolean isSelectOnFocus() {
        return nIsSelectOnFocus(mAddress);
    }

    public APLTextProperties getTextProperties(@NonNull IMetricsTransform metricsTransform) {
        return new APLTextProperties(nGetTextProperties(mAddress), metricsTransform);
    }

    private static native int nGetHighlightColor(long address);
    private static native int nGetTextColor(long address);
    private static native int nGetKeyboardType(long address);
    private static native int nGetSubmitKeyType(long address);
    private static native String nStrip(byte[] text, long address);
    private static native long nGetTextProperties(long address);
    private static native boolean nIsSecureInput(long address);
    private static native boolean nIsSelectOnFocus(long address);
}