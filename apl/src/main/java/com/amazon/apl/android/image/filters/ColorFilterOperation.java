/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.ColorFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.Collections;

/**
 * Apply an overlay color on the image bitmap
 */
public class ColorFilterOperation extends FilterOperation {
    ColorFilterOperation(Filters.Filter filter, IBitmapFactory bitmapFactory) {
        super(Collections.emptyList(), filter, bitmapFactory);
    }

    /**
     * Returns a {@link FilterResult} that is a Color.
     */
    @Override
    public FilterResult call() {
        return new ColorFilterResult(getFilter().color(), getBitmapFactory());
    }
}
