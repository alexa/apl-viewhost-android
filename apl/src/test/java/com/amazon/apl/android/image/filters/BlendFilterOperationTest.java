/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.renderscript.ScriptIntrinsicBlend;

import com.amazon.apl.android.image.filters.bitmap.ColorFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.BlendMode;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.ImageScale;

import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlendFilterOperationTest extends RenderScriptOperationTest<BlendFilterOperation, ScriptIntrinsicBlend> {
    @Mock
    private ScriptIntrinsicBlend mScriptIntrinsic;

    @Override
    public Class<ScriptIntrinsicBlend> getScriptClass() {
        return ScriptIntrinsicBlend.class;
    }

    @Override
    public ScriptIntrinsicBlend getScript() {
        return mScriptIntrinsic;
    }

    @Test
    public void test_mode_normal() {
        init(Arrays.asList(createDummyBitmap(), createDummyBitmap()),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeBlend)
                        .source(0)
                        .destination(1)
                        .blendMode(BlendMode.kBlendModeNormal)
                        .build());
        Bitmap source = getSourceAt(0).getBitmap();
        Bitmap dest = getSourceAt(1).getBitmap();
        when(mRenderScript.createFromBitmap(eq(source), any(), anyInt())).thenReturn(mAllocIn);
        when(mRenderScript.createFromBitmap(eq(dest), any(), anyInt())).thenReturn(mAllocOut);

        FilterResult result = getFilterOperation().call();
        assertNotNull(result.getBitmap());

        verify(mScriptIntrinsic).forEachSrcOver(eq(mAllocIn), eq(mAllocOut));
    }

    @Test
    public void test_mode_multiply() {
        init(Arrays.asList(createDummyBitmap(), createDummyBitmap()),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeBlend)
                        .source(0)
                        .destination(1)
                        .blendMode(BlendMode.kBlendModeMultiply)
                        .build());
        Bitmap source = getSourceAt(0).getBitmap();
        Bitmap dest = getSourceAt(1).getBitmap();
        when(mRenderScript.createFromBitmap(eq(source), any(), anyInt())).thenReturn(mAllocIn);
        when(mRenderScript.createFromBitmap(eq(dest), any(), anyInt())).thenReturn(mAllocOut);

        FilterResult result = getFilterOperation().call();
        assertNotNull(result.getBitmap());

        verify(mScriptIntrinsic).forEachMultiply(eq(mAllocIn), eq(mAllocOut));
    }

    @Test
    public void test_getWithException_returnsTransparentBitmap() throws Exception {
        Future<FilterResult> thrownFuture = mock(Future.class);
        when(thrownFuture.get(any(long.class), any(TimeUnit.class))).thenThrow(new ExecutionException("thrown", new Exception()));

        List<Future<FilterResult>> thrownFutures = Arrays.asList(thrownFuture, thrownFuture);
        Filters.Filter filter = Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeBlend)
                        .source(0)
                        .destination(1)
                        .blendMode(BlendMode.kBlendModeNormal)
                        .build();

        mFilterOperation = FilterOperationFactory.create(thrownFutures, filter, mBitmapFactory, mRenderScript, mExtensionImageFilterCallback, Size.ZERO, ImageScale.kImageScaleNone);

        ColorFilterResult source = (ColorFilterResult) mFilterOperation.getSource();
        assertEquals(Color.TRANSPARENT, source.getColor());

        ColorFilterResult destination = (ColorFilterResult) mFilterOperation.getDestination();
        assertEquals(Color.TRANSPARENT, destination.getColor());

        ColorFilterResult filterResult = (ColorFilterResult) mFilterOperation.call();
        assertEquals(Color.TRANSPARENT, filterResult.getColor());
    }
}
