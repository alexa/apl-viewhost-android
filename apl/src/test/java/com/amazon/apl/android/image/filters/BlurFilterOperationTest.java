/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.renderscript.Allocation;
import android.renderscript.ScriptIntrinsicBlur;

import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;

import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class BlurFilterOperationTest extends RenderScriptOperationTest<BlurFilterOperation, ScriptIntrinsicBlur> {
    @Mock
    ScriptIntrinsicBlur mScriptIntrinsicBlur;

    @Override
    public Class<ScriptIntrinsicBlur> getScriptClass() {
        return ScriptIntrinsicBlur.class;
    }

    @Override
    public ScriptIntrinsicBlur getScript() {
        return mScriptIntrinsicBlur;
    }

    @Test
    public void testBlurFilterOperation() throws Exception {
        init(Collections.singletonList(createDummyBitmap()),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeBlur)
                        .radius(10f)
                        .build());
        FilterResult source = getSourceAt(0);

        when(mRenderScript.createFromBitmap(eq(source.getBitmap()), any(), anyInt())).thenReturn(mAllocIn);
        when(mRenderScript.createFromBitmap(eq(mResultBitmap), any(), anyInt())).thenReturn(mAllocOut);
        when(mRenderScript.createScript(any(), eq(ScriptIntrinsicBlur.class))).thenReturn(mScriptIntrinsicBlur);

        FilterResult result = getFilterOperation().call();
        assertEquals(mResultBitmap, result.getBitmap());

        verify(mRenderScript).createFromBitmap(eq(source.getBitmap()), eq(Allocation.MipmapControl.MIPMAP_NONE), eq(Allocation.USAGE_SCRIPT));
        verify(mRenderScript).createFromBitmap(eq(mResultBitmap), eq(Allocation.MipmapControl.MIPMAP_NONE), eq(Allocation.USAGE_SCRIPT));
        verify(mRenderScript).createScript(any(), eq(ScriptIntrinsicBlur.class));
        verify(mScriptIntrinsicBlur).setRadius(10f);
        verify(mScriptIntrinsicBlur).setInput(mAllocIn);
        verify(mScriptIntrinsicBlur).forEach(mAllocOut);
        verify(mAllocOut).copyTo(mResultBitmap);
    }
}