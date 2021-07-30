/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.renderscript.FieldPacker;
import android.renderscript.Matrix4f;
import android.renderscript.ScriptIntrinsicColorMatrix;

import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ColorMatrixFilterOperationTest extends RenderScriptOperationTest<ColorMatrixFilterOperation, ScriptIntrinsicColorMatrix> {
    @Mock
    ScriptIntrinsicColorMatrix mScriptIntrinsicColorMatrix;
    ArgumentCaptor<FieldPacker> mFieldPackerCaptor = ArgumentCaptor.forClass(FieldPacker.class);

    @Override
    public Class<ScriptIntrinsicColorMatrix> getScriptClass() {
        return ScriptIntrinsicColorMatrix.class;
    }

    @Override
    public ScriptIntrinsicColorMatrix getScript() {
        return mScriptIntrinsicColorMatrix;
    }

    @Test
    public void testGrayScale_zero_isIdentity() throws Exception {
        init(Collections.singletonList(createDummyBitmap()),
                Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeGrayscale)
                .amount(0.0f)
                .source(-1)
                .build());

        setupRs();

        FilterResult filterResult = getFilterOperation().call();
        verifySuccess(filterResult);

        FieldPacker expected = new FieldPacker(16*4);
        expected.addMatrix(new Matrix4f());
        assertArrayEquals(expected.getData(), mFieldPackerCaptor.getValue().getData());
    }

    @Test
    public void testGrayScale_one() throws Exception {
        init(Collections.singletonList(createDummyBitmap()),
                Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeGrayscale)
                .amount(1.0f)
                .source(-1)
                .build());

        setupRs();

        FilterResult filterResult = getFilterOperation().call();
        verifySuccess(filterResult);

        Matrix4f expectedMatrix = new Matrix4f();
        expectedMatrix.set(0, 0, 1f - 0.701f);
        expectedMatrix.set(1, 0, 0.587f);
        expectedMatrix.set(2, 0, 0.114f);
        expectedMatrix.set(0, 1, 0.299f);
        expectedMatrix.set(1, 1, 1f - 0.413f);
        expectedMatrix.set(2, 1, 0.114f);
        expectedMatrix.set(0, 2, 0.299f);
        expectedMatrix.set(1, 2, 0.587f);
        expectedMatrix.set(2, 2, 1f - 0.886f);
        FieldPacker expected = new FieldPacker(16*4);
        expected.addMatrix(expectedMatrix);
        assertArrayEquals(expected.getData(), mFieldPackerCaptor.getValue().getData());
    }

    @Test
    public void testSaturate_zero() throws Exception {
        init(Collections.singletonList(createDummyBitmap()),
                Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeSaturate)
                .amount(0.0f)
                .source(-1)
                .build());

        setupRs();

        FilterResult filterResult = getFilterOperation().call();
        verifySuccess(filterResult);

        Matrix4f expectedMatrix = new Matrix4f();
        expectedMatrix.set(0, 0, 0.299f);
        expectedMatrix.set(1, 0, 0.587f);
        expectedMatrix.set(2, 0, 0.114f);
        expectedMatrix.set(0, 1, 0.299f);
        expectedMatrix.set(1, 1, 0.587f);
        expectedMatrix.set(2, 1, 0.114f);
        expectedMatrix.set(0, 2, 0.299f);
        expectedMatrix.set(1, 2, 0.587f);
        expectedMatrix.set(2, 2, 0.114f);
        FieldPacker expected = new FieldPacker(16 * 4);
        expected.addMatrix(expectedMatrix);
        assertArrayEquals(expected.getData(), mFieldPackerCaptor.getValue().getData());
    }

    @Test
    public void testSaturate_one() throws Exception {
        init(Collections.singletonList(createDummyBitmap()),
                Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeSaturate)
                .amount(1.0f)
                .source(-1)
                .build());

        setupRs();

        FilterResult filterResult = getFilterOperation().call();
        verifySuccess(filterResult);

        Matrix4f expectedMatrix = new Matrix4f();
        FieldPacker expected = new FieldPacker(16 * 4);
        expected.addMatrix(expectedMatrix);
        assertArrayEquals(expected.getData(), mFieldPackerCaptor.getValue().getData());
    }

    private void setupRs() {
        FilterResult source = getSourceAt(0);
        when(mRenderScript.createFromBitmap(eq(source.getBitmap()), any(), anyInt())).thenReturn(mAllocIn);
        when(mRenderScript.createFromBitmap(eq(mResultBitmap), any(), anyInt())).thenReturn(mAllocOut);
    }

    private void verifySuccess(FilterResult filterResult) {
        assertEquals(mResultBitmap, filterResult.getBitmap());

        verify(mScriptIntrinsicColorMatrix).setVar(eq(0), mFieldPackerCaptor.capture());
        verify(mScriptIntrinsicColorMatrix).forEach(eq(mAllocIn), eq(mAllocOut));
        verify(mAllocOut).copyTo(mResultBitmap);
    }
}
