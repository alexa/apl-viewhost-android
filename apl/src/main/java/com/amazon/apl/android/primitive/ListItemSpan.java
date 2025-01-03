/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.primitive;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple implementation of bulleted list item span.
 */
public class ListItemSpan implements LeadingMarginSpan {
    private static final String DEFAULT_BULLET = " \u2022 ";
    private final int mBulletWidth;

    public ListItemSpan(@NonNull TextPaint textPaint) {
        mBulletWidth = (int) Math.ceil(textPaint.measureText(DEFAULT_BULLET));
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mBulletWidth;
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas canvas, @NonNull Paint paint, int x, int dir,
                                  int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end,
                                  boolean first, @Nullable Layout layout) {
        if (((Spanned) text).getSpanStart(this) == start) {
            Paint.Style style = paint.getStyle();

            paint.setStyle(Paint.Style.FILL);

            // If dir is negative the margin is to the right of the text.
            if (dir < 0) {
                canvas.drawText(DEFAULT_BULLET, x - mBulletWidth, baseline, paint);
            } else {
                canvas.drawText(DEFAULT_BULLET, x , baseline, paint);
            }
            paint.setStyle(style);
        }
    }

    @Override
    public String toString() {
        return String.format("ListItemSpan{bulletWidth=%d}", mBulletWidth);
    }
}
