/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.ScriptIntrinsic;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.List;
import java.util.concurrent.Future;

public abstract class RenderscriptFilterOperation<T extends ScriptIntrinsic> extends FilterOperation {
    protected final RenderScriptWrapper mRenderscriptWrapper;
    private FilterBitmaps mBitmaps;

    RenderscriptFilterOperation(List<Future<FilterResult>> sourceFutures, Filters.Filter filter, IBitmapFactory bitmapFactory, RenderScriptWrapper renderScriptWrapper) {
        super(sourceFutures, filter, bitmapFactory);
        mRenderscriptWrapper = renderScriptWrapper;
    }

    /**
     * Get the script object.
     * @param element the element from the Allocation.
     * @return the script object.
     */
    abstract T getScript(Element element);

    /**
     * Gets the function to apply to the script.
     * @return the function to apply to the script.
     */
    abstract ScriptActor<T> getScriptActor();

    @Override
    public FilterResult call() throws Exception {
        Allocation source = null;
        Allocation destination = null;
        T script = null;
        try {
            mBitmaps = createFilterBitmaps();
            source = mRenderscriptWrapper.createFromBitmap(mBitmaps.source(), Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            destination = mRenderscriptWrapper.createFromBitmap(mBitmaps.destination(), Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            script = getScript(source.getElement());
            getScriptActor().act(script, source, destination);
            Bitmap resultBitmap = mBitmaps.result();
            destination.copyTo(resultBitmap);
            return new BitmapFilterResult(resultBitmap, getBitmapFactory());
        } finally {
            if (source != null) {
                source.destroy();
                source = null;
            }

            if (destination != null) {
                destination.destroy();
                destination = null;
            }

            if (script != null) {
                script.destroy();
                script = null;
            }

            // Trigger gc event since memory is not stored in java
            // This prevents ANRs when many large images are filtered
            System.runFinalization();
            System.gc();
        }
    }

    /**
     * Actor to act on a {@link ScriptIntrinsic} object.
     * @param <T> the type of {@link ScriptIntrinsic}.
     */
    interface ScriptActor<T extends ScriptIntrinsic> {
        void act(T scriptIntrinsic, Allocation allocIn, Allocation allocOut);
    }
}
