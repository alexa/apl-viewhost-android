/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.image.filters.FilterExecutor;
import com.amazon.apl.android.image.filters.RenderScriptWrapper;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.image.filters.bitmap.Size;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;
import com.google.auto.value.AutoValue;

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
    private static final String METRIC_FILTER_INTERRUPTED = METRIC_FILTER_SUCCESS + ".interrupted" + ITelemetryProvider.FAIL_SUFFIX;

    private static final String TAG = "ImageProcessingTask";

    @Override
    protected ImageProcessingAsyncParams doInBackground(ImageProcessingAsyncParams... p) {
        ImageProcessingAsyncParams params = p[0];
        // Set up last bitmap to return in case we're cancelled.
        params.getOnFinishRunnable().onFailure(params.getBitmaps().get(params.getBitmaps().size() - 1));
        if (isCancelled()) {
            return params;
        }

        // Send bitmaps to preprocessor first
        List<Bitmap> preprocessedBitmaps = params.getImageProcessor().preProcessImage(params.getSources(), params.getBitmaps());
        if (isCancelled()) {
            return params;
        }

        // create "ImageProcessing" metric
        ITelemetryProvider telemetryProvider = params.getTelemetryProvider();
        int cMetricFilterSuccess = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_SUCCESS, ITelemetryProvider.Type.COUNTER);
        int cMetricFilterTimeout = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_TIMEOUT, ITelemetryProvider.Type.COUNTER);
        int cMetricFilterExecution = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_EXECUTION, ITelemetryProvider.Type.COUNTER);
        int cMetricFilterInterrupted = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_FILTER_INTERRUPTED, ITelemetryProvider.Type.COUNTER);
        int imageProcessingMetricId = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, IMAGE_PROCESSING_METRIC_NAME, ITelemetryProvider.Type.TIMER);

        // TODO this overall timer metric will only give us one per document and will have a lot of noise
        //  due to variation in size and complexity of filters
        telemetryProvider.startTimer(imageProcessingMetricId);
        // Apply the filters
        FilterExecutor filterExecutor = FilterExecutor.create(sExecutorService, preprocessedBitmaps, params.getFilters(), params.getBitmapFactory(), params.getRenderScriptWrapper(), params.getExtensionImageFilterCallback());
        Bitmap filteredResult;
        try {
            FilterResult result = filterExecutor.apply();
            if (result.isBitmap()) {
                filteredResult = result.getBitmap();
            } else {
                filteredResult = result.getBitmap(Size.create(params.getBounds().intWidth(), params.getBounds().intHeight()));
            }

            telemetryProvider.incrementCount(cMetricFilterSuccess);
            telemetryProvider.stopTimer(imageProcessingMetricId);
        } catch (TimeoutException e) {
            telemetryProvider.incrementCount(cMetricFilterTimeout);
            Log.e(TAG, String.format("Filter processing took longer than %d seconds. Returning last bitmap.", FilterExecutor.TIMEOUT_SECONDS), e);
            telemetryProvider.fail(imageProcessingMetricId);
            filteredResult = preprocessedBitmaps.get(preprocessedBitmaps.size() - 1);
        } catch (InterruptedException e) {
            telemetryProvider.incrementCount(cMetricFilterInterrupted);
            Log.e(TAG, "Filter thread interrupted. Returning last bitmap.", e);
            telemetryProvider.fail(imageProcessingMetricId);
            filteredResult = preprocessedBitmaps.get(preprocessedBitmaps.size() - 1);
        } catch (ExecutionException e) {
            telemetryProvider.incrementCount(cMetricFilterExecution);
            Log.e(TAG, "Error processing filters. Returning last bitmap.", e);
            telemetryProvider.fail(imageProcessingMetricId);
            filteredResult = preprocessedBitmaps.get(preprocessedBitmaps.size() - 1);
        }

        params.getOnFinishRunnable().onFailure(filteredResult);
        if (isCancelled()) {
            return params;
        }

        // By now we have applied all filters successfully.
        try {
            Bitmap scaledBitmap = ImageScaler.getScaledBitmap(
                    params.getBounds(),
                    params.getImageScale(),
                    params.getImageAlign(),
                    params.getBitmapFactory(),
                    filteredResult);

            /**
             * Order is important as per spec:
             * The overlayColor filter will be applied after any filters defined by the filter property
             * and before the overlayGradient property.
             *
             * Furthermore, this should be done after bitmap scaling to ensure that the gradient is the
             * correct width and height.
             */
            if (scaledBitmap == null) {
                return params;
            }

            int overlayColor = params.getOverlayColor();
            if (overlayColor != Color.TRANSPARENT) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(overlayColor);
                paint.setAlpha(Color.alpha(overlayColor));
                // Passing a bitmap into canvas requires a mutable bitmap so make a mutable copy if this one
                // is not mutable.
                if (!scaledBitmap.isMutable()) {
                    scaledBitmap = params.getBitmapFactory().copy(scaledBitmap, true);
                }
                Canvas canvas = new Canvas(scaledBitmap);
                canvas.drawRect(new Rect(0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight()), paint);
            }
            if (isCancelled()) {
                return params;
            }

            Gradient overlayGradient = params.getOverlayGradient();
            if (overlayGradient != null) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(overlayGradient.getShader(scaledBitmap.getWidth(), scaledBitmap.getHeight()));
                // Passing a bitmap into canvas requires a mutable bitmap so make a mutable copy if this one
                // is not mutable.
                if (!scaledBitmap.isMutable()) {
                    scaledBitmap = params.getBitmapFactory().copy(scaledBitmap, true);
                }
                Canvas canvas = new Canvas(scaledBitmap);
                canvas.drawRect(new Rect(0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight()), paint);
            }
            if (isCancelled()) {
                return params;
            }

            params.getOnFinishRunnable().onSuccess(scaledBitmap);
        } catch (BitmapCreationException e) {
            params.getOnFinishRunnable().onFailure(filteredResult);
        }

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
        p.getOnFinishRunnable().run();
    }

    @AutoValue
    public abstract static class ImageProcessingAsyncParams {
        public abstract List<String> getSources();
        public abstract Filters getFilters();
        public abstract com.amazon.apl.android.primitive.Rect getBounds();
        public abstract ImageScale getImageScale();
        public abstract ImageAlign getImageAlign();
        public abstract IBitmapFactory getBitmapFactory();
        public abstract List<Bitmap> getBitmaps();
        public abstract IImageProcessor getImageProcessor();
        public abstract ITelemetryProvider getTelemetryProvider();
        public abstract ImageViewAdapter.BitmapRunnable getOnFinishRunnable();
        public abstract IExtensionImageFilterCallback getExtensionImageFilterCallback();
        public abstract RenderScriptWrapper getRenderScriptWrapper();
        @Nullable
        public abstract Gradient getOverlayGradient();
        public abstract int getOverlayColor();

        public static ImageProcessingAsyncParams.Builder builder() {
            return new AutoValue_ImageProcessingAsyncTask_ImageProcessingAsyncParams.Builder()
                    .filters(Filters.create())
                    .overlayColor(Color.TRANSPARENT);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract ImageProcessingAsyncParams.Builder sources(List<String> sources);
            public abstract ImageProcessingAsyncParams.Builder imageProcessor(IImageProcessor imageProcessor);
            public abstract ImageProcessingAsyncParams.Builder filters(Filters filters);
            public abstract ImageProcessingAsyncParams.Builder bounds(com.amazon.apl.android.primitive.Rect bounds);
            public abstract ImageProcessingAsyncParams.Builder imageScale(ImageScale imageScale);
            public abstract ImageProcessingAsyncParams.Builder imageAlign(ImageAlign imageAlign);
            public abstract ImageProcessingAsyncParams.Builder bitmapFactory(IBitmapFactory bitmapFactory);
            public abstract ImageProcessingAsyncParams.Builder bitmaps(List<Bitmap> bitmaps);
            public abstract ImageProcessingAsyncParams.Builder telemetryProvider(ITelemetryProvider telemetryProvider);
            public abstract ImageProcessingAsyncParams.Builder onFinishRunnable(ImageViewAdapter.BitmapRunnable onFinishRunnable);
            public abstract ImageProcessingAsyncParams.Builder extensionImageFilterCallback(IExtensionImageFilterCallback callback);
            public abstract ImageProcessingAsyncParams.Builder renderScriptWrapper(RenderScriptWrapper renderScriptWrapper);
            public abstract ImageProcessingAsyncParams.Builder overlayGradient(Gradient gradient);
            public abstract ImageProcessingAsyncParams.Builder overlayColor(int color);

            public abstract ImageProcessingAsyncParams build();
        }
    }
}
