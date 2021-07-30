/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.NoiseFilterKind;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class NoiseFilterOperationTest extends FilterOperationTest<NoiseFilterOperation> {
    NoiseFilterOperation mNoiseOperation;

    @Test
    public void testNoiseFilterOperation() {
        FilterResult source = createDummyBitmap();

        mNoiseOperation = spy(new NoiseFilterOperation(
                Collections.singletonList(new MockFuture(source)),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeNoise)
                        .source(0)
                        .noiseKind(NoiseFilterKind.kFilterNoiseKindUniform)
                        .noiseSigma(15f)
                        .noiseUseColor(true)
                        .build(),
                mBitmapFactory));

        doNothing().when(mNoiseOperation).setNoiseSeed(anyInt());
        doNothing().when(mNoiseOperation).noiseFilter(any(), anyInt(), anyBoolean(), anyBoolean());

        BitmapFilterResult filterResult = (BitmapFilterResult) mNoiseOperation.call();
        assertNotNull(filterResult);
        // TODO in NoiseFilterOperation why this is no longer true.
//        assertNotEquals(filterResult.getBitmap(), source.getBitmap());

        verify(mNoiseOperation).setNoiseSeed(42);
        verify(mNoiseOperation).noiseFilter(eq(filterResult.getBitmap()), eq(15), eq(true), eq(true));
    }
}
