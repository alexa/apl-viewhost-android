/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible for applying all the filters to an Image.
 */
public class FilterExecutor {
    public static final int TIMEOUT_SECONDS = 60;
    private static final String TAG = "FilterExecutor";

    private final ExecutorService mExecutorService;
    private final Filters mFilters;
    private final RenderScriptWrapper mRenderScript;
    private final IExtensionImageFilterCallback mExtensionImageFilterCallback;
    private final IBitmapFactory mBitmapFactory;
    private final List<Future<FilterResult>> mFilterResultFutures = new ArrayList<>();

    private FilterExecutor(ExecutorService executorService, List<Bitmap> sourceBitmaps, Filters filters, IBitmapFactory bitmapFactory, RenderScriptWrapper renderScript, IExtensionImageFilterCallback extensionImageFilterCallback) {
        mExecutorService = executorService;
        mFilters = filters;
        mRenderScript = renderScript;
        mExtensionImageFilterCallback = extensionImageFilterCallback;
        mBitmapFactory = bitmapFactory;
        // Initialize source bitmaps
        for (final Bitmap source : sourceBitmaps) {
            mFilterResultFutures.add(mExecutorService.submit(() -> new BitmapFilterResult(source, bitmapFactory)));
        }
    }

    /**
     * Creates a FilterExecutor to process Image filters on source bitmaps.
     *
     * @param executorService               The executor to process filters on
     * @param sourceBitmaps                 The source bitmaps (immutable)
     * @param filters                       The filters to process
     * @param renderScript                  RenderScript for fast filter processing
     * @param extensionImageFilterCallback  The callback for ExtensionFilters.
     * @return                              A FilterExecutor
     */
    public static FilterExecutor create(ExecutorService executorService,
                                 List<Bitmap> sourceBitmaps,
                                 Filters filters,
                                 IBitmapFactory bitmapFactory,
                                 RenderScriptWrapper renderScript,
                                 IExtensionImageFilterCallback extensionImageFilterCallback) {
        return new FilterExecutor(executorService, sourceBitmaps, filters, bitmapFactory, renderScript, extensionImageFilterCallback);
    }

    /**
     * Applies all the filters and returns the result.
     *
     * @return the Filtered result.
     * @throws ExecutionException   If a particular filter throws an exception.
     * @throws InterruptedException If a filter operation is interrupted.
     * @throws TimeoutException     If the Filters take more than 60 seconds to execute.
     */
    public FilterResult apply() throws ExecutionException, InterruptedException, TimeoutException {
        Future<FilterResult> result = mFilterResultFutures.get(mFilterResultFutures.size() - 1);
        for (int i = 0; i < mFilters.size(); i++) {
            Filters.Filter filter = mFilters.at(i);
            result = mExecutorService.submit(
                    FilterOperationFactory.create(
                            getSourceFilterResults(filter),
                            filter,
                            mBitmapFactory,
                            mRenderScript,
                            mExtensionImageFilterCallback));
            mFilterResultFutures.add(result);
        }
        // Free up any bitmaps not needed.
        FilterResult ret = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mExecutorService.submit(() -> {
            for (Future<FilterResult> future : mFilterResultFutures) {
                if (future.isDone()) {
                    Bitmap b = null;
                    try {
                        b = future.get().getBitmap();
                    } catch (ExecutionException | InterruptedException e) {
                        // do nothing
                    }
                    if (b != ret.getBitmap() && !b.isRecycled()) {
                        mBitmapFactory.disposeBitmap(b);
                    }
                }
            }
            mFilterResultFutures.clear();
        });
        return ret;
    }

    /**
     * Retrieves the source filter results needed by a particular filter.
     * @param filter    a filter
     * @return          a list of source future filter results
     */
    private List<Future<FilterResult>> getSourceFilterResults(Filters.Filter filter) {
        List<Future<FilterResult>> sourceFilterResults = new ArrayList<>();
        Integer sourceIdx = filter.source();
        if (sourceIdx != null) {
            sourceFilterResults.add(getFutureFilterResult(sourceIdx));
        }

        Integer destIdx = filter.destination();
        if (destIdx != null) {
            sourceFilterResults.add(getFutureFilterResult(destIdx));
        }

        return sourceFilterResults;
    }

    private Future<FilterResult> getFutureFilterResult(int wrappedIndex) {
        int index = wrappedIndex >= 0 ? wrappedIndex : wrappedIndex + mFilterResultFutures.size();
        return mFilterResultFutures.get(index);
    }
}
