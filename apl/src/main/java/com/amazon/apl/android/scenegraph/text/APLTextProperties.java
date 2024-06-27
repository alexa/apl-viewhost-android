/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.scenegraph.text;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.ITextProxy;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;

/**
 * Text properties container.
 */
public class APLTextProperties implements ITextProxy {
    private final long mNativeHandle;

    private IMetricsTransform mMetricsTransform;

    public APLTextProperties(long nativeHandle, @NonNull IMetricsTransform metricsTransform) {
        mNativeHandle = nativeHandle;
        mMetricsTransform = metricsTransform;
    }

    @Override
    public TextAlign getTextAlign() {
        return TextAlign.valueOf(nGetTextAlign(mNativeHandle));
    }

    @Override
    public String getFontFamily() {
        return nGetFontFamily(mNativeHandle);
    }

    @Override
    public int getFontWeight() {
        return nGetFontWeight(mNativeHandle);
    }

    @Override
    public String getFontLanguage() { return nGetFontLanguage(mNativeHandle); }

    @Override
    public FontStyle getFontStyle() {
        return FontStyle.valueOf(nGetFontStyle(mNativeHandle));
    }

    @Override
    public float getFontSize() {
        // Actually dimension. Required to be converted to pixels
        return mMetricsTransform.toViewhost(nGetFontSize(mNativeHandle));
    }

    @Nullable
    @Override
    public Dimension getLetterSpacing() {
        // Actually dimension. Required to be converted to pixels
        return Dimension.create(mMetricsTransform.toViewhost(nGetLetterSpacing(mNativeHandle)));
    }

    @Override
    public int getMaxLines() {
        return nGetMaxLines(mNativeHandle);
    }

    @Override
    public float getLineHeight() {
        return nGetLineHeight(mNativeHandle);
    }

    @Override
    public TextAlignVertical getTextAlignVertical() {
        return TextAlignVertical.valueOf(nGetTextAlignVertical(mNativeHandle));
    }

    @Override
    public float getScalingFactor() {
        return mMetricsTransform.toViewhost(1.f);
    }

    @Override
    public String getVisualHash() {
        return String.valueOf(nGetHash(mNativeHandle));
    }

    private static native int nGetTextAlign(long nativeHandle);
    private static native String nGetFontFamily(long nativeHandle);
    private static native int nGetFontWeight(long nativeHandle);
    private static native int nGetFontStyle(long nativeHandle);
    private static native String nGetFontLanguage(long nativeHandle);
    private static native float nGetFontSize(long nativeHandle);
    private static native float nGetLetterSpacing(long nativeHandle);
    private static native int nGetMaxLines(long nativeHandle);
    private static native float nGetLineHeight(long nativeHandle);
    private static native int nGetTextAlignVertical(long nativeHandle);
    private static native long nGetHash(long nativeHandle);
}
