/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.renderscript.Element;
import android.renderscript.ScriptIntrinsicBlend;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.enums.BlendMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Filter operation for Blend filter
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-filters.html#blend
 */
public class BlendFilterOperation extends RenderscriptFilterOperation<ScriptIntrinsicBlend> {
    private static final String TAG = "BlendFilterOperation";
    private static final Map<BlendMode, ScriptActor<ScriptIntrinsicBlend>> mModeActorMap = new HashMap<>();
    static {
        mModeActorMap.put(BlendMode.kBlendModeNormal, ScriptIntrinsicBlend::forEachSrcOver);
        mModeActorMap.put(BlendMode.kBlendModeMultiply, ScriptIntrinsicBlend::forEachMultiply);
    }

    BlendFilterOperation(List<Future<FilterResult>> sourceFutures, Filters.Filter filter, IBitmapFactory bitmapFactory, @NonNull RenderScriptWrapper renderScript) {
        super(sourceFutures, filter, bitmapFactory, renderScript);
    }

    FilterBitmaps createFilterBitmaps() throws BitmapCreationException {
        FilterResult source = getSource();
        FilterResult destination = getDestination();
        if (source == null || destination == null) {
            throw new IllegalArgumentException(TAG + ": Source and destination can't be null.");
        }

        Bitmap sourceBitmap = null;
        Bitmap destinationBitmap = null;
        Bitmap resultBitmap = null;
        if (destination.isBitmap()) {
            // If destination is a bitmap, then pad/truncate the source to fit into the destination
            sourceBitmap = source.getBitmap(destination.getSize());
            destinationBitmap = destination.getBitmap();
        } else if (source.isBitmap()) {
            // If destination is not a bitmap, then scale it to be the size of source
            sourceBitmap = source.getBitmap();
            destinationBitmap = destination.getBitmap(source.getSize());
        }

        if (destinationBitmap != null) {
            resultBitmap = getBitmapFactory().createBitmap(destinationBitmap.getWidth(), destinationBitmap.getHeight());
        }

        return FilterBitmaps.create(sourceBitmap, destinationBitmap, resultBitmap);
    }

    @Override
    ScriptIntrinsicBlend getScript(Element element) {
        return mRenderscriptWrapper.createScript(element, ScriptIntrinsicBlend.class);
    }

    @Nullable
    @Override
    ScriptActor<ScriptIntrinsicBlend> getScriptActor() {
        return mModeActorMap.get(getFilter().blendMode());
    }

    @Override
    public FilterResult call() {
        try {
            // If renderscript hasn't been implemented yet, we fallback to the slower Blender implementation.
            if (getScriptActor() == null) {
                FilterBitmaps bitmaps = createFilterBitmaps();
                return new BitmapFilterResult(Blender.performBlending(bitmaps.source(), bitmaps.destination(), bitmaps.result(), getFilter().blendMode()), getBitmapFactory());
            } else {
                return super.call();
            }
        } catch (Exception e) {
            // If there is an exception, then we return the destination.
            Log.e(TAG, "Exception processing blend filter.", e);
            return getDestination();
        }
    }
}
