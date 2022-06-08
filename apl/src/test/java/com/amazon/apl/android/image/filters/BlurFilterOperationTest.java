/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.ScriptIntrinsicBlur;

import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.ImageScale;

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
    public void testBlurFilterOperation_scaleHalfBestFill() throws Exception {
        int width = 100;
        int height = 100;
        // Image view size is double, so blur should be half
        float blurRadius = 10f;
        float expectedBlurRadius = 5f;
        Size imageViewSize = Size.create(width*2, height*2);
        performBlurTest(width, height, imageViewSize, blurRadius, expectedBlurRadius, ImageScale.kImageScaleBestFill);
    }

    @Test
    public void testBlurFilterOperation_scaleNone() throws Exception {
        int width = 100;
        int height = 100;
        // Image view size is double, but no scaling blur should not change
        float blurRadius = 10f;
        float expectedBlurRadius = 10f;
        Size imageViewSize = Size.create(width*2, height*2);
        performBlurTest(width, height, imageViewSize, blurRadius, expectedBlurRadius, ImageScale.kImageScaleNone);
    }

    @Test
    public void testBlurFilterOperation_scaleBestFitOnImageViewWithOffsetDimensions_takesMinimumBlur() throws Exception {
        int width = 100;
        int height = 100;
        // Image view size is double the width and four times the height, blur should take minimum scaling
        // which is half the blur, since the image will be twice as big on screen
        float blurRadius = 10f;
        float expectedBlurRadius = 5f;
        Size imageViewSize = Size.create(width*2, height*4);
        performBlurTest(width, height, imageViewSize, blurRadius, expectedBlurRadius, ImageScale.kImageScaleBestFit);
    }

    @Test
    public void testBlurFilterOperation_scaleFillDifferentAspectRatio_takesMinimumBlur() throws Exception {
        int width = 100;
        int height = 100;
        // Image view size is double the width, blur should take minimum scaling
        float blurRadius = 10f;
        float expectedBlurRadius = 10f;
        Size imageViewSize = Size.create(width*2, height);
        performBlurTest(width, height, imageViewSize, blurRadius, expectedBlurRadius, ImageScale.kImageScaleFill);
    }

    @Test
    public void testBlurFilterOperation_blurRadiusAbove25_clamps() throws Exception {
        int width = 100;
        int height = 100;
        float blurRadius = 50f;
        float expectedBlurRadius = 25f;
        Size imageViewSize = Size.create(width, height);
        performBlurTest(width, height, imageViewSize, blurRadius, expectedBlurRadius, ImageScale.kImageScaleBestFill);
    }

    @Test
    public void testBlurFilterOperation_blurRadiusBelow0_clamps() throws Exception {
        int width = 100;
        int height = 100;
        float blurRadius = -0.1f;
        float expectedBlurRadius = 0.000000001f;
        Size imageViewSize = Size.create(width*2, height);
        performBlurTest(width, height, imageViewSize, blurRadius, expectedBlurRadius, ImageScale.kImageScaleBestFill);
    }

    private void performBlurTest(int bitmapWidth,
                                 int bitmapHeight,
                                 Size imageViewSize,
                                 float blurRadius,
                                 float expectedBlurRadius,
                                 ImageScale imageScale) throws Exception {
        init(Collections.singletonList(new BitmapFilterResult(Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888), mBitmapFactory)),
                Filters.Filter.builder()
                        .filterType(FilterType.kFilterTypeBlur)
                        .radius(blurRadius)
                        .build(),
                imageViewSize,
                imageScale);
        FilterResult source = getSourceAt(0);

        when(mRenderScript.createFromBitmap(eq(source.getBitmap()), any(), anyInt())).thenReturn(mAllocIn);
        when(mRenderScript.createFromBitmap(eq(mResultBitmap), any(), anyInt())).thenReturn(mAllocOut);
        when(mRenderScript.createScript(any(), eq(ScriptIntrinsicBlur.class))).thenReturn(mScriptIntrinsicBlur);

        FilterResult result = getFilterOperation().call();
        assertEquals(mResultBitmap, result.getBitmap());

        verify(mRenderScript).createFromBitmap(eq(source.getBitmap()), eq(Allocation.MipmapControl.MIPMAP_NONE), eq(Allocation.USAGE_SCRIPT));
        verify(mRenderScript).createFromBitmap(eq(mResultBitmap), eq(Allocation.MipmapControl.MIPMAP_NONE), eq(Allocation.USAGE_SCRIPT));
        verify(mRenderScript).createScript(any(), eq(ScriptIntrinsicBlur.class));
        verify(mScriptIntrinsicBlur).setRadius(expectedBlurRadius);
        verify(mScriptIntrinsicBlur).setInput(mAllocIn);
        verify(mScriptIntrinsicBlur).forEach(mAllocOut);
        verify(mAllocOut).copyTo(mResultBitmap);
    }
}