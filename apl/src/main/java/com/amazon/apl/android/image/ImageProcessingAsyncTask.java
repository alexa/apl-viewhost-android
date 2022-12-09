/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amazon.apl.android.utils.ConcurrencyUtils;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.image.filters.FilterExecutor;
import com.amazon.apl.android.image.filters.RenderScriptWrapper;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.enums.ImageScale;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class ImageProcessingAsyncTask extends AsyncTask<ImageProcessingAsyncTask.ImageProcessingAsyncParams, Void, ImageProcessingAsyncTask.ImageProcessingAsyncParams> {

    private static final ExecutorService sExecutorService = Executors.newFixedThreadPool(4);
    private static final String IMAGE_PROCESSING_METRIC_NAME = "ImageProcessing";
    private static final String METRIC_FILTER_SUCCESS = "ImageFilter";
    private static final String METRIC_FILTER_TIMEOUT = METRIC_FILTER_SUCCESS + ".timeout" + ITelemetryProvider.FAIL_SUFFIX;
    private static final String METRIC_FILTER_EXECUTION = METRIC_FILTER_SUCCESS + ".execution" + ITelemetryProvider.FAIL_SUFFIX;

    private static final String TAG = "ImageProcessingTask";

    /**
     * The result bitmap to show when finished processing.
     */
    private Bitmap mResult;

    @Override
    protected ImageProcessingAsyncParams doInBackground(ImageProcessingAsyncParams... p) {
        ImageProcessingAsyncParams params = p[0];
        // Set up last bitmap to return in case we're cancelled.
        mResult = params.getBitmaps().get(params.getBitmaps().size() - 1);
        if (isCancelled()) {
            return params;
        }


        List<Bitmap> sourceBitmaps = params.getBitmaps();
        // We make copies here due to not wanting to modify Glide's original bitmaps
        List<Bitmap> bitmapsToProcess = new ArrayList<>(sourceBitmaps.size());
        for (int i = 0; i < sourceBitmaps.size(); i++) {
            Bitmap original = sourceBitmaps.get(i);
            try {
                bitmapsToProcess.add(params.getBitmapFactory().createBitmap(original));
            } catch (BitmapCreationException e) {
                Log.e(TAG, "Unable to make copy for image processing. Not applying filters.", e);
                return params;
            }
        }

        // Send bitmaps to preprocessor first
        if (params.getImageProcessor() != null) {
            bitmapsToProcess = params.getImageProcessor().preProcessImage(params.getSources(), bitmapsToProcess);
            if (isCancelled()) {
                return params;
            }
        }

        // create "ImageProcessing" metric
        ITelemetryProvider telemetryProvider = params.getTelemetryProvider();
        int cMetricFilterSuccess = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_SUCCESS, ITelemetryProvider.Type.COUNTER);
        int cMetricFilterTimeout = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_TIMEOUT, ITelemetryProvider.Type.COUNTER);
        int cMetricFilterExecution = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_EXECUTION, ITelemetryProvider.Type.COUNTER);
        int imageProcessingMetricId = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, IMAGE_PROCESSING_METRIC_NAME, ITelemetryProvider.Type.TIMER);

        // TODO this overall timer metric will only give us one per document and will have a lot of noise
        //  due to variation in size and complexity of filters
        telemetryProvider.startTimer(imageProcessingMetricId);
        // Apply the filters
        Size imageSize = Size.create(params.getBounds().intWidth(), params.getBounds().intHeight());
        FilterExecutor filterExecutor = FilterExecutor.create(sExecutorService,
                bitmapsToProcess,
                params.getFilters(),
                params.getBitmapFactory(),
                params.getRenderScriptWrapper(),
                params.getExtensionImageFilterCallback(),
                params.getImageScale(),
                imageSize);
        Bitmap filteredResult;
        try {
            FilterResult result = filterExecutor.apply();
            if (result.isBitmap()) {
                filteredResult = result.getBitmap();
            } else {
                filteredResult = result.getBitmap(imageSize);
            }

            telemetryProvider.incrementCount(cMetricFilterSuccess);
            telemetryProvider.stopTimer(imageProcessingMetricId);
        } catch (TimeoutException e) {
            telemetryProvider.incrementCount(cMetricFilterTimeout);
            Log.e(TAG, String.format("Filter processing took longer than %d seconds. Returning last bitmap.", ConcurrencyUtils.LARGE_TIMEOUT_SECONDS), e);
            telemetryProvider.fail(imageProcessingMetricId);
            filteredResult = bitmapsToProcess.get(bitmapsToProcess.size() - 1);
        } catch (InterruptedException e) {
            Log.i(TAG, "Filter thread interrupted. Returning last bitmap.", e);
            filteredResult = bitmapsToProcess.get(bitmapsToProcess.size() - 1);
        } catch (ExecutionException e) {
            telemetryProvider.incrementCount(cMetricFilterExecution);
            Log.e(TAG, "Error processing filters. Returning last bitmap.", e);
            telemetryProvider.fail(imageProcessingMetricId);
            filteredResult = bitmapsToProcess.get(bitmapsToProcess.size() - 1);
        }

        mResult = filteredResult;
        if (isCancelled()) {
            return params;
        }

        // Store the filtered result in the cache
        params.getBitmapCache().putBitmap(params.getImageBitmapKey(), filteredResult);

        return params;
    }

    @Override
    protected void onCancelled(ImageProcessingAsyncParams p) {
        if (p != null) {
            p.getRenderScriptWrapper().destroy();
        }
    }

    @Override
    protected void onPostExecute(ImageProcessingAsyncParams p) {
        p.getRenderScriptWrapper().destroy();
        p.getOnProcessingFinished().accept(mResult);
    }

    @AutoValue
    public abstract static class ImageProcessingAsyncParams {
        public abstract List<String> getSources();
        public abstract Filters getFilters();
        public abstract com.amazon.apl.android.primitive.Rect getBounds();
        public abstract IBitmapFactory getBitmapFactory();
        public abstract List<Bitmap> getBitmaps();
        public abstract IBitmapCache getBitmapCache();
        public abstract ProcessedImageBitmapKey getImageBitmapKey();
        @Nullable
        public abstract IImageProcessor getImageProcessor();
        public abstract ITelemetryProvider getTelemetryProvider();
        public abstract ImageViewAdapter.ImageProcessingFinished getOnProcessingFinished();
        public abstract IExtensionImageFilterCallback getExtensionImageFilterCallback();
        public abstract RenderScriptWrapper getRenderScriptWrapper();
        public abstract ImageScale getImageScale();

        public static ImageProcessingAsyncParams.Builder builder() {
            return new AutoValue_ImageProcessingAsyncTask_ImageProcessingAsyncParams.Builder()
                    .filters(Filters.create());
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract ImageProcessingAsyncParams.Builder sources(List<String> sources);
            public abstract ImageProcessingAsyncParams.Builder imageProcessor(IImageProcessor imageProcessor);
            public abstract ImageProcessingAsyncParams.Builder filters(Filters filters);
            public abstract ImageProcessingAsyncParams.Builder bounds(com.amazon.apl.android.primitive.Rect bounds);
            public abstract ImageProcessingAsyncParams.Builder bitmapFactory(IBitmapFactory bitmapFactory);
            public abstract ImageProcessingAsyncParams.Builder bitmapCache(IBitmapCache cache);
            public abstract ImageProcessingAsyncParams.Builder bitmaps(List<Bitmap> bitmaps);
            public abstract ImageProcessingAsyncParams.Builder telemetryProvider(ITelemetryProvider telemetryProvider);
            public abstract ImageProcessingAsyncParams.Builder onProcessingFinished(ImageViewAdapter.ImageProcessingFinished onProcessingFinished);
            public abstract ImageProcessingAsyncParams.Builder extensionImageFilterCallback(IExtensionImageFilterCallback callback);
            public abstract ImageProcessingAsyncParams.Builder renderScriptWrapper(RenderScriptWrapper renderScriptWrapper);
            public abstract ImageProcessingAsyncParams.Builder imageBitmapKey(ProcessedImageBitmapKey bitmapKey);
            public abstract ImageProcessingAsyncParams.Builder imageScale(ImageScale imageScale);

            public abstract ImageProcessingAsyncParams build();
        }
    }
}
