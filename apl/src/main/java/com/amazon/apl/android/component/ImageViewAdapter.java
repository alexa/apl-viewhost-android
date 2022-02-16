/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Looper;
import android.renderscript.RenderScript;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.functional.Consumer;
import com.amazon.apl.android.image.ImageProcessingAsyncTask;
import com.amazon.apl.android.image.ProcessedImageBitmapKey;
import com.amazon.apl.android.image.filters.RenderScriptProvider;
import com.amazon.apl.android.image.filters.RenderScriptWrapper;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.LazyImageLoader;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.ImageScale;
import com.amazon.apl.enums.PropertyKey;

import java.util.List;

public class ImageViewAdapter extends ComponentViewAdapter<Image, APLImageView> {
    private static final String TAG = "ImageViewAdapter";
    private static ImageViewAdapter INSTANCE;

    public static final String METRIC_COUNTER_BITMAP_CACHE_HIT = "ImageBitmapCacheHit";
    public static final String METRIC_COUNTER_BITMAP_CACHE_MISS = "ImageBitmapCacheMiss";

    private ImageViewAdapter() {
        super();
        putPropertyFunction(PropertyKey.kPropertyOverlayGradient, this::applyOverlayGradient);
        putPropertyFunction(PropertyKey.kPropertyOverlayColor, this::applyOverlayColor);
        putPropertyFunction(PropertyKey.kPropertySource, this::initImageLoading);
        putPropertyFunction(PropertyKey.kPropertyBorderRadius, this::applyBorderRadius);
        putPropertyFunction(PropertyKey.kPropertyAlign, this::applyAlign);
        putPropertyFunction(PropertyKey.kPropertyScale, this::applyScale);
    }

    public static ImageViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ImageViewAdapter();
        }
        return INSTANCE;
    }

    @Override
    public APLImageView createView(Context context, IAPLViewPresenter presenter) {
        APLImageView view = new APLImageView(context, presenter);
        return view;
    }

    @Override
    void applyPadding(Image component, APLImageView view) {
        setPaddingFromBounds(component, view, false);
    }

    @Override
    public void applyAllProperties(Image component, APLImageView view) {
        super.applyAllProperties(component, view);
        applyAlign(component, view);
        applyScale(component, view);
        applyBorderRadius(component, view);
        applyOverlayGradient(component, view);
        applyOverlayColor(component, view);
        initImageLoading(component, view);
    }

    private void applyAlign(Image component, APLImageView view) {
        view.setImageAlign(component.getAlign());
    }

    private void applyScale(Image component, APLImageView view) {
        ImageScale newScale = component.getScale();
        ImageScale currentScale = view.getImageScale();
        // When changing scale to/from none we need to do a fresh load
        if (view.getDrawable() != null &&
                newScale != currentScale &&
                (newScale == ImageScale.kImageScaleNone || currentScale == ImageScale.kImageScaleNone)) {
            initImageLoading(component, view);
        }

        view.setImageScale(component.getScale());
    }

    private void applyBorderRadius(Image component, APLImageView view) {
        Dimension borderRadius = component.getBorderRadius();
        if (borderRadius != null) {
            view.setBorderRadius(borderRadius.value());
        }
    }

    private void applyOverlayColor(Image component, APLImageView view) {
        view.setOverlayColor(component.getOverlayColor());
    }

    private void applyOverlayGradient(Image component, APLImageView view) {
        view.setOverlayGradient(component.getOverlayGradient());
    }

    /**
     * Initialize loading of images.
     * @param image
     * @param view
     */
    private void initImageLoading(Image image, @NonNull APLImageView view) {
        APLTrace trace = image.getViewPresenter().getAPLTrace();
        trace.startTrace(TracePoint.IMAGE_INIT_IMAGE_LOAD);
        final ProcessedImageBitmapKey imageBitmapKey = ProcessedImageBitmapKey.create(image);
        final Bitmap cachedResult = image.getRenderingContext().getBitmapCache().getBitmap(imageBitmapKey);

        final ITelemetryProvider telemetryProvider = image.getRenderingContext().getTelemetryProvider();
        final int metricBitmapCacheHitCounter = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_COUNTER_BITMAP_CACHE_HIT, ITelemetryProvider.Type.COUNTER);
        final int metricBitmapCacheMissCounter = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_COUNTER_BITMAP_CACHE_MISS, ITelemetryProvider.Type.COUNTER);
        if (cachedResult != null) {
            createAndSetDrawableToImageView(view, image, cachedResult);
            telemetryProvider.incrementCount(metricBitmapCacheHitCounter);
            trace.endTrace();
            return;
        }

        telemetryProvider.incrementCount(metricBitmapCacheMissCounter);
        LazyImageLoader.initImageLoad(this, image, view);
        trace.endTrace();
    }

    /**
     * Callback for when images are loaded for an Image component.
     * @param view       the view to draw into
     * @param sourceBmps the source bitmaps
     */
    public void onImageLoad(@NonNull APLImageView view, @NonNull List<Bitmap> sourceBmps) {
        if (sourceBmps.isEmpty()) {
            return;
        }

        final Image image = (Image) view.getPresenter().findComponent(view);
        if (image == null) {
            return;
        }

        final boolean needsProcessing = image.getFilters().size() > 0 || image.getImageProcessor() != null;
        drawBitmapOnCanvas(image, view, sourceBmps, needsProcessing);
    }

    private void drawBitmapOnCanvas(Image image, @NonNull APLImageView view, @NonNull List<Bitmap> sourceBmps, boolean needsProcessing) {
        if (needsProcessing) {
            ImageProcessingAsyncTask.ImageProcessingAsyncParams.Builder builder = ImageProcessingAsyncTask.ImageProcessingAsyncParams.builder()
                    .sources(image.getSourceUrls())
                    .bounds(image.getInnerBounds())
                    .bitmapFactory(image.getBitmapFactory())
                    .bitmaps(sourceBmps)
                    .imageProcessor(image.getImageProcessor())
                    .telemetryProvider(image.getRenderingContext().getTelemetryProvider())
                    .onProcessingFinished((Bitmap result) -> createAndSetDrawableToImageView(view, image, result))
                    .extensionImageFilterCallback(image.getExtensionImageFilterCallback())
                    .filters(image.getFilters())
                    .imageBitmapKey(ProcessedImageBitmapKey.create(image))
                    .bitmapCache(image.getRenderingContext().getBitmapCache())
                    .renderScriptWrapper(new RenderScriptWrapper(new RenderScriptProvider(RenderScript::create, view.getContext())));

            ImageProcessingAsyncTask.ImageProcessingAsyncParams params = builder.build();

            clearAsyncTask(view);
            ImageProcessingAsyncTask asyncTask = new ImageProcessingAsyncTask();
            view.setImageProcessingAsyncTask(asyncTask);
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            final Bitmap result = sourceBmps.get(sourceBmps.size() - 1);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                createAndSetDrawableToImageView(view, image, result);
            } else {
                view.post(() -> createAndSetDrawableToImageView(view, image, result));
            }
        }
    }

    /**
     * Cancel pending or running async tasks and clear them.
     */
    public synchronized void clearAsyncTask(APLImageView imageView) {
        AsyncTask asyncTask = imageView.getImageProcessingAsyncTask();
        if (asyncTask != null) {
            asyncTask.cancel(true);
            imageView.setImageProcessingAsyncTask(null);
        }
    }

    @UiThread
    private void createAndSetDrawableToImageView(@NonNull APLImageView view, @NonNull Image image, @NonNull Bitmap bitmap) {
        // Setting the drawable on the ImageView triggers a requestLayout() call
        // because it thinks the width/height of the ImageView are changing.
        //
        // For APL Image (without a shadow) the bounds are fixed and don't depend on the Drawable,
        // meaning the layout pass is not necessary.
        //
        // To avoid it we temporarily ignore layout requests.
        view.setLayoutRequestsEnabled(image.shouldDrawBoxShadow());

        view.setImageDrawable(new BitmapDrawable(view.getResources(), bitmap));
        view.setImageProcessingAsyncTask(null);

        view.setLayoutRequestsEnabled(true);
    }

    /**
     * Function to execute on result of Bitmap processing.
     */
    public interface ImageProcessingFinished extends Consumer<Bitmap> { }
}
