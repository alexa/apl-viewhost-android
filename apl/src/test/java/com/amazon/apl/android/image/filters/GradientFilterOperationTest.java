/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import com.amazon.apl.android.image.filters.bitmap.GradientFilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.enums.FilterType;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class GradientFilterOperationTest extends FilterOperationTest<GradientFilterOperation> {

    @Test
    public void testGradientFilterOperation() {
        Gradient gradient = mock(Gradient.class);

        init(Collections.emptyList(),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeGradient)
                        .gradient(gradient)
                        .build());

        GradientFilterResult gradientFilterResult = (GradientFilterResult) getFilterOperation().call();
        assertEquals(gradient, gradientFilterResult.getGradient());
    }
}
