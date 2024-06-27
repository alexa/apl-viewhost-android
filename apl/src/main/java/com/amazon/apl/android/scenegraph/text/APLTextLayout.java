/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.scenegraph.text;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.text.Layout;

import com.amazon.apl.android.primitive.StyledText;
import com.amazon.apl.android.scaling.IMetricsTransform;

import java.nio.charset.StandardCharsets;

/**
 * Wrapper class to hold the text layout created during measurement
 * All dimensions such as width, height etc. must be in Core dp units
 */
public class APLTextLayout {
    private final Layout mLayout;
    private final StyledText mStyledText;
    private APLTextProperties mTextProperties;
    private final float mWidthDp;
    private final float mHeightDp;
    private final boolean mLinesClipped;

    private IMetricsTransform mMetricsTransform;

    @SuppressLint("WrongConstant")
    public APLTextLayout(final Layout layout, final StyledText styledText, boolean linesClipped, float widthDp, float heightDp) {
        mLayout = layout;
        mStyledText = styledText;
        mWidthDp = widthDp;
        mHeightDp = heightDp;
        mLinesClipped = linesClipped;
    }

    public void attachTextProperties(final APLTextProperties aplTextProperties) {
        mTextProperties = aplTextProperties;
    }

    public void applyMetricsTransform(IMetricsTransform metricsTransform) {
        mMetricsTransform = metricsTransform;
    }

    // Following are overrides of apl::TextLayout to be accessed by JNI layer.
    @SuppressWarnings("unused")
    private int getLineCount() {
        return mLayout.getLineCount();
    }

    @SuppressWarnings("unused")
    public float[] getSize() {
        return new float[]{mWidthDp, mHeightDp};
    }

    /**
     * Called from JNI APLTextLayout
     * @return the byte length
     */
    @SuppressWarnings("unused")
    private int getByteLength() {
        return mStyledText.getText(null, mMetricsTransform).toString().getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Called from JNI APLTextLayout
     * @return the bounding box for a range of lines
     */
    @SuppressWarnings("unused")
    private int[] getBoundingBoxForLineRange(int lowerBound, int upperBound) {
        Rect boundingBox = new Rect(0,0,0,0);
        for (int line = lowerBound; line <= upperBound; line++) {
            if (boundingBox.isEmpty()) {
                mLayout.getLineBounds(line, boundingBox);
            } else {
                Rect rect = new Rect();
                mLayout.getLineBounds(line, rect);
                boundingBox.union(rect);
            }
        }
        return new int[]{boundingBox.left, boundingBox.top, boundingBox.width(), boundingBox.height()};
    }

    /**
     * Called from JNI APLTextLayout
     * @return the range of lines from a specified byte range
     */
    @SuppressWarnings("unused")
    private int[] getLineRangeFromByteRange(int rangeStart, int rangeEnd) {
        int lineStart = mLayout.getLineForOffset(rangeStart);
        int lineEnd = mLayout.getLineForOffset(rangeEnd);
        return new int[]{lineStart, lineEnd};
    }

    /**
     * Called from JNI APLTextLayout
     * @return the baseline
     */
    @SuppressWarnings("unused")
    private double getBaseLine() {
        return mLayout.getLineBaseline(0);
    }

    @SuppressWarnings("unused")
    private boolean isTruncated() {
        return mLinesClipped ||  mLayout.getEllipsisCount(mLayout.getLineCount() - 1) > 0;
    }

    @SuppressWarnings("unused")
    private String getLaidOutText() {
        return mLayout.getText().toString();
    }

    public Layout getLayout() {
        return mLayout;
    }

    public APLTextProperties getTextProperties() {
        return mTextProperties;
    }
}
