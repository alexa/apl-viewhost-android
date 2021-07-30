/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.renderscript.Element;
import android.renderscript.ScriptIntrinsicBlur;
import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Apply a Gaussian blur with a specified radius.
 */
public class BlurFilterOperation extends RenderscriptFilterOperation<ScriptIntrinsicBlur> {
    private static final String TAG = "BlurFilterOperation";
    public static final float MIN_RADIUS = 0.000000001f;
    public static final float MAX_RADIUS = 25f;

    BlurFilterOperation(List<Future<FilterResult>> sourceFutures, Filters.Filter filter, IBitmapFactory bitmapFactory, RenderScriptWrapper renderScript) {
        super(sourceFutures, filter, bitmapFactory, renderScript);
    }

    FilterBitmaps createFilterBitmaps() throws BitmapCreationException {
        FilterResult source = getSource();
        if (source == null || !source.isBitmap()) {
            throw new IllegalArgumentException(TAG + ": Source bitmap must be an actual bitmap.");
        }

        Bitmap sourceBitmap = source.getBitmap();
        Bitmap destinationBitmap = getBitmapFactory().createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight());
        return FilterBitmaps.create(sourceBitmap, destinationBitmap, destinationBitmap);
    }

    @Override
    ScriptIntrinsicBlur getScript(Element element) {
        ScriptIntrinsicBlur scriptIntrinsicBlur = mRenderscriptWrapper.createScript(element, ScriptIntrinsicBlur.class);
        scriptIntrinsicBlur.setRadius(getFilter().radius());
        return scriptIntrinsicBlur;
    }

    @Nullable
    @Override
    ScriptActor<ScriptIntrinsicBlur> getScriptActor() {
        return ((scriptIntrinsic, allocIn, allocOut) -> {
            scriptIntrinsic.setInput(allocIn);
            scriptIntrinsic.forEach(allocOut);
        });
    }
}