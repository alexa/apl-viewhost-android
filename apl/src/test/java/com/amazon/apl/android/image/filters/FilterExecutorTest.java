/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.ImageScale;
import com.amazon.apl.enums.NoiseFilterKind;

import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilterExecutorTest extends ViewhostRobolectricTest {
    @Mock
    ExecutorService mExecutorService;
    @Mock
    IBitmapFactory mBitmapFactory;
    @Mock
    RenderScriptWrapper mRenderScript;
    @Mock
    IExtensionImageFilterCallback mExtensionImageFilterCallback;

    List<Bitmap> mSourceBitmaps;
    Filters mFilters;
    FilterExecutor mFilterExecutor;

    List<Callable<FilterResult>> mFilterOperations = new ArrayList<>();
    List<Future<FilterResult>> mFutureFilterResults = new ArrayList<>();
    List<FilterResult> mFilterResults = new ArrayList<>();
    int index = 0;

    @Test
    public void test_noFilters_returnsLastSource() throws Exception {
        mSourceBitmaps = Arrays.asList(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        mFilters = Filters.create();
        init();

        verify(mExecutorService, times(2)).submit(any(Callable.class));
        assertEquals(mSourceBitmaps.get(0), mFilterOperations.get(0).call().getBitmap());
        assertEquals(mSourceBitmaps.get(1), mFilterOperations.get(1).call().getBitmap());

        FilterResult filterResult = mFilterExecutor.apply();
        assertEquals(mFilterResults.get(1), filterResult);
    }

    @Test
    public void test_filters_applySequentially() throws Exception {
        mSourceBitmaps = Arrays.asList(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        mFilters = Filters.create();
        mFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeColor)
                .color(Color.RED).build());
        mFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeBlend)
                .source(0)
                .destination(1)
                .build());
        mFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeNoise)
                .source(-1)
                .noiseSigma(10f)
                .noiseUseColor(false)
                .noiseKind(NoiseFilterKind.kFilterNoiseKindGaussian)
                .build());
        init();
        assertEquals(mSourceBitmaps.get(0), mFilterOperations.get(0).call().getBitmap());

        // Check result of apply is last
        FilterResult filterResult = mFilterExecutor.apply();
        assertEquals(mFilterResults.get(3), filterResult);

        // First filter is color
        ColorFilterOperation colorFilterOperation = (ColorFilterOperation) mFilterOperations.get(1);
        assertEquals(mFilters.at(0), colorFilterOperation.getFilter());
        assertNull(colorFilterOperation.getSource());
        assertNull(colorFilterOperation.getDestination());

        // Second filter is blend
        BlendFilterOperation blendFilterOperation = (BlendFilterOperation) mFilterOperations.get(2);
        assertEquals(mFilters.at(1), blendFilterOperation.getFilter());
        assertEquals(mFilterResults.get(0), blendFilterOperation.getSource());
        assertEquals(mFilterResults.get(1), blendFilterOperation.getDestination());

        // Last filter is noise
        NoiseFilterOperation noiseFilterOperation = (NoiseFilterOperation) mFilterOperations.get(3);
        assertEquals(mFilters.at(2), noiseFilterOperation.getFilter());
        assertEquals(mFilterResults.get(2), noiseFilterOperation.getSource());
        assertNull(noiseFilterOperation.getDestination());

        // Verify disposal
        verify(mBitmapFactory, never()).disposeBitmap(filterResult.getBitmap());
        for (int i = 0; i < mFilterResults.size() - 1; i++) {
            verify(mBitmapFactory).disposeBitmap(eq(mFilterResults.get(i).getBitmap()));
        }
    }
    
    void init() {
        // init mocks
        for (int i = 0; i < mSourceBitmaps.size() + mFilters.size(); i++) {
            FilterResult filterResult = mock(FilterResult.class);
            Future<FilterResult> resultFuture = mock(Future.class);
            when(filterResult.isBitmap()).thenReturn(true);
            when(filterResult.getBitmap()).thenReturn(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            try {
                when(resultFuture.isDone()).thenReturn(true);
                when(resultFuture.get(anyLong(), any())).thenReturn(filterResult);
                when(resultFuture.get()).thenReturn(filterResult);
            } catch (Exception e) {

            }
            mFilterResults.add(filterResult);
            mFutureFilterResults.add(resultFuture);
        }

        when(mExecutorService.submit(any(Callable.class))).thenAnswer(invocation -> {
            mFilterOperations.add(invocation.getArgument(0));
            return mFutureFilterResults.get(index++);
        });


        mFilterExecutor = FilterExecutor.create(
                mExecutorService,
                mSourceBitmaps,
                mFilters,
                mBitmapFactory,
                mRenderScript,
                mExtensionImageFilterCallback,
                ImageScale.kImageScaleNone,
                Size.ZERO);
    }
}
