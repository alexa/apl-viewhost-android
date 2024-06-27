/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import static java.lang.Math.min;

import android.graphics.Bitmap;
import android.renderscript.Element;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.ImageScaleCalculator;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.ImageScale;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Apply a Gaussian blur with a specified radius.
 */
public class BlurFilterOperation extends RenderscriptFilterOperation<ScriptIntrinsicBlur> {
    private static final String TAG = "BlurFilterOperation";
    private static final float MIN_RADIUS = 0.000000001f;
    private static final float MAX_RADIUS = 25f;

    private final Size mImageSize;
    private final ImageScale mImageScale;

    private final Size mTargetSize;
    private final float mRadiusInAbsoluteDimensions;

    BlurFilterOperation(List<Future<FilterResult>> sourceFutures, Filters.Filter filter,
                        IBitmapFactory bitmapFactory,
                        RenderScriptWrapper renderScript,
                        Size imageSize,
                        ImageScale imageScale
                        ) {
        super(sourceFutures, filter, bitmapFactory, renderScript);
        mImageScale = imageScale;
        mImageSize = imageSize;
        mRadiusInAbsoluteDimensions = 0f;
        mTargetSize = null;
    }

    public BlurFilterOperation(List<Future<FilterResult>> sourceFutures, Filters.Filter filter,
                        IBitmapFactory bitmapFactory,
                        RenderScriptWrapper renderScript, float radiusInAbsoluteDimensions, Size targetSize) {
        super(sourceFutures, filter, bitmapFactory, renderScript);
        mImageScale = null;
        mImageSize = null;
        mRadiusInAbsoluteDimensions = Math.min(BlurFilterOperation.MAX_RADIUS, Math.max(BlurFilterOperation.MIN_RADIUS, radiusInAbsoluteDimensions));
        mTargetSize = targetSize;
    }

    FilterBitmaps createFilterBitmaps() throws BitmapCreationException {
        FilterResult source = getSource();
        if (source == null || !source.isBitmap()) {
            throw new IllegalArgumentException(TAG + ": Source bitmap must be an actual bitmap.");
        }

        Bitmap sourceBitmap = mTargetSize == null ? source.getBitmap() : source.getBitmap(mTargetSize);
        Bitmap destinationBitmap = getBitmapFactory().createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight());
        return FilterBitmaps.create(sourceBitmap, destinationBitmap, destinationBitmap);
    }

    @Override
    ScriptIntrinsicBlur getScript(Element element) {
        FilterResult source = getSource();
        if (source == null || !source.isBitmap()) {
            throw new IllegalArgumentException(TAG + ": Source bitmap must be an actual bitmap.");
        }


        ScriptIntrinsicBlur scriptIntrinsicBlur = mRenderscriptWrapper.createScript(element, ScriptIntrinsicBlur.class);
        if (mRadiusInAbsoluteDimensions != 0f) {
            scriptIntrinsicBlur.setRadius(mRadiusInAbsoluteDimensions);
        } else {
            Bitmap sourceBitmap = source.getBitmap();
            // Blur needs to take into account scaling done on the image size, since it is
            // an "Absolute Dimension" (See spec here https://aplspec.aka.corp.amazon.com/release-1.9/html/filters.html#blur)
            // meaning it applies the radius based on the screen size not the original image size.
            float[] scaleWidthHeight = ImageScaleCalculator.getScale(mImageScale, mImageSize.width(), mImageSize.height(), sourceBitmap.getWidth(), sourceBitmap.getHeight());
            final float scalingFactor = min(scaleWidthHeight[0], scaleWidthHeight[1]);
            // Render script does not support a radius larger than 25px.
            final float clampedRadius = Math.min(BlurFilterOperation.MAX_RADIUS, Math.max(BlurFilterOperation.MIN_RADIUS, getFilter().radius() / scalingFactor));
            scriptIntrinsicBlur.setRadius(clampedRadius);
        }
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