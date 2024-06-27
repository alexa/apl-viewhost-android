/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.GradientFilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;

import java.util.Collections;

/**
 * Apply a specified gradient overlay on the image bitmap
 */
public class GradientFilterOperation extends FilterOperation {
    private final Gradient mGradient;

    GradientFilterOperation(Filters.Filter filter, IBitmapFactory bitmapFactory) {
        this(filter.gradient(), bitmapFactory);
    }

    GradientFilterOperation(Gradient gradient, IBitmapFactory bitmapFactory) {
        super(Collections.emptyList(), null, bitmapFactory);
        mGradient = gradient;
    }

    @Override
    public FilterResult call() {
        return new GradientFilterResult(mGradient, getBitmapFactory());
    }
}
