/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Color;

import com.amazon.apl.android.image.filters.bitmap.ColorFilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ColorFilterOperationTest extends FilterOperationTest<ColorFilterOperation> {

    @Test
    public void testColorFilter() {
        init(Collections.emptyList(),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeColor)
                        .color(Color.BLUE)
                        .build());

        ColorFilterResult result = (ColorFilterResult) getFilterOperation().call();
        assertEquals(Color.BLUE, result.getColor());
    }
}
