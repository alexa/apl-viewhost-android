/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.GradientFilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.Collections;

/**
 * Apply a specified gradient overlay on the image bitmap
 */
public class GradientFilterOperation extends FilterOperation {

    GradientFilterOperation(Filters.Filter filter, IBitmapFactory bitmapFactory) {
        super(Collections.emptyList(), filter, bitmapFactory);
    }

    @Override
    public FilterResult call() {
        return new GradientFilterResult(getFilter().gradient(), getBitmapFactory());
    }
}
