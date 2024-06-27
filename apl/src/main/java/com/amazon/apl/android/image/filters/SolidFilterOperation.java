/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Paint;
import android.graphics.Rect;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.GradientFilterResult;
import com.amazon.apl.android.image.filters.bitmap.PaintFilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;

import java.util.Collections;

/**
 * Apply a specified gradient overlay on the image bitmap
 */
public class SolidFilterOperation extends FilterOperation {
    private final PaintProvider mPaintProvider;

    public SolidFilterOperation(PaintProvider paintProvider, IBitmapFactory bitmapFactory) {
        super(Collections.emptyList(), null, bitmapFactory);
        mPaintProvider = paintProvider;
    }

    @Override
    public FilterResult call() {
        return new PaintFilterResult(mPaintProvider, getBitmapFactory());
    }

    public interface PaintProvider {
        Paint getPaint(Rect rect);
    }
}
