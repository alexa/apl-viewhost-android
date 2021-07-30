/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.text;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Just holds the span for the current highlighted line
 */
public class LineSpan {
    private final int start;
    private final int end;
    private final int color;

    public LineSpan(int start, int end, int color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getColor() {
        return color;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        LineSpan other = (LineSpan) obj;
        return other.start == start && other.end == end && other.color == color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, color);
    }
}
