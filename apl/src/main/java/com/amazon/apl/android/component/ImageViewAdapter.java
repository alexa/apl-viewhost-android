/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.image.ImageBitmapKey;
import com.amazon.apl.android.image.ImageProcessingAsyncTask;
import com.amazon.apl.android.image.ImageScaleCalculator;
import com.amazon.apl.android.image.filters.RenderScriptWrapper;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.utils.LazyImageLoader;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.PropertyKey;

import java.util.ArrayList;
import java.util.List;

public class ImageViewAdapter extends ComponentViewAdapter<Image, APLImageView> {
    private static final String TAG = "ImageViewAdapter";
    private static ImageViewAdapter INSTANCE;

    public static final String METRIC_COUNTER_BITMAP_CACHE_HIT = "ImageBitmapCacheHit";
    public static final String METRIC_COUNTER_BITMAP_CACHE_MISS = "ImageBitmapCacheMiss";

    private ImageViewAdapter() {
        super();
        // TODO update refine applying properties for Image
        putPropertyFunction(PropertyKey.kPropertyOverlayColor, this::initImageLoading);
        putPropertyFunction(PropertyKey.kPropertySource, this::initImageLoading);
        putPropertyFunction(PropertyKey.kPropertyAlign, this::initImageLoading);
        putPropertyFunction(PropertyKey.kPropertyBorderRadius, this::initImageLoading);
        putPropertyFunction(PropertyKey.kPropertyScale, this::initImageLoading);
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
        initImageLoading(component, view);
    }

    @Override
    public void requestLayout(Image component, APLImageView view) {
        super.requestLayout(component, view);
        initImageLoading(component, view);
    }

    /**
     * Initialize loading of images.
     * @param image
     * @param view
     */
    private void initImageLoading(Image image, @NonNull APLImageView view) {
        final RenderingContext renderingContext = image.getRenderingContext();
        final ITelemetryProvider telemetryProvider = renderingContext.getTelemetryProvider();
        final IBitmapCache bitmapCache = renderingContext.getBitmapCache();

        final int metricBitmapCacheHitCounter = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_COUNTER_BITMAP_CACHE_HIT, ITelemetryProvider.Type.COUNTER);
        final int metricBitmapCacheMissCounter = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_COUNTER_BITMAP_CACHE_MISS, ITelemetryProvider.Type.COUNTER);

        final Bitmap cachedBitmap = bitmapCache.getBitmap(ImageBitmapKey.create(image));

        if(cachedBitmap != null) {
            setDrawableToImageView(view, image, cachedBitmap, true);
            telemetryProvider.incrementCount(metricBitmapCacheHitCounter);
            return;
        }

        telemetryProvider.incrementCount(metricBitmapCacheMissCounter);
        LazyImageLoader.lazyLoadImage(this, image, view);
    }

    /**
     * Callback for when images are loaded for an Image component.
     * @param view       the view to draw into
     * @param sourceBmps the source bitmaps
     */
    public void onImageLoad(@NonNull APLImageView view, @NonNull List<Bitmap> sourceBmps, boolean skipCache) {
        if (sourceBmps.isEmpty()) {
            return;
        }

        final Image image = (Image) view.getPresenter().findComponent(view);
        if (image == null) {
            return;
        }

        boolean shouldApplyFilters = true;
        IBitmapFactory bitmapFactory = image.getBitmapFactory();
        final List<Bitmap> sourceCopy = new ArrayList<>(sourceBmps.size());
        for (int i = 0; i < sourceBmps.size(); i++) {
            Bitmap original = sourceBmps.get(i);
            try {
                sourceCopy.add(bitmapFactory.createBitmap(original));
            } catch (BitmapCreationException e) {
                Log.e(TAG, "Unable to make copy for image processing. Not applying filters.", e);
                // Don't apply filters to the original
                shouldApplyFilters = false;
                sourceCopy.add(original);
            }
        }

        drawBitmapOnCanvas(view, sourceCopy, shouldApplyFilters, skipCache);
    }

    private void drawBitmapOnCanvas(@NonNull APLImageView view, @NonNull List<Bitmap> sourceBmps, boolean shouldApplyFilters, boolean skipCache) {
        // Process image in an async task.
        Image image = (Image) view.getPresenter().findComponent(view);
        if (image == null) {
            return;
        }

        // Do not proceed if the list is empty.
        if (sourceBmps.isEmpty()) {
            return;
        }

        ImageProcessingAsyncTask.ImageProcessingAsyncParams.Builder builder = ImageProcessingAsyncTask.ImageProcessingAsyncParams.builder()
                .sources(image.getSources())
                .bounds(image.getInnerBounds())
                .imageScale(image.getScale())
                .imageAlign(image.getAlign())
                .bitmapFactory(image.getBitmapFactory())
                .bitmaps(sourceBmps)
                .imageProcessor(image.getImageProcessor())
                .telemetryProvider(image.getRenderingContext().getTelemetryProvider())
                .onFinishRunnable(new BitmapRunnable(this, view, image, skipCache))
                .extensionImageFilterCallback(image.getExtensionImageFilterCallback())
                .renderScriptWrapper(new RenderScriptWrapper(view.getContext()));
        if (shouldApplyFilters) {
            // These can impact the original bitmaps, so in a situation near OOM we don't apply them.
            builder.overlayColor(image.getOverlayColor())
                    .overlayGradient(image.getOverlayGradient())
                    .filters(image.getFilters());
        }

        ImageProcessingAsyncTask.ImageProcessingAsyncParams params = builder.build();

        clearAsyncTask(true, view);
        ImageProcessingAsyncTask asyncTask = new ImageProcessingAsyncTask();
        view.setImageProcessingAsyncTask(asyncTask);
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    /**
     * Cancel pending or running async tasks and clear them.
     */
    public synchronized void clearAsyncTask(boolean mayInterruptIfRunning, APLImageView imageView) {
        AsyncTask asyncTask = imageView.getImageProcessingAsyncTask();
        if (asyncTask != null) {
            asyncTask.cancel(mayInterruptIfRunning);
            imageView.setImageProcessingAsyncTask(null);
        }
    }

    private void setDrawableToImageView(@NonNull APLImageView view, @NonNull Image image, @NonNull Bitmap scaledBitmap, boolean skipCache) {
        if (!skipCache) {
            image.getRenderingContext().getBitmapCache().putBitmap(ImageBitmapKey.create(image), scaledBitmap);
        }

        view.setImageDrawable(getBitmapDrawable(scaledBitmap, image, view));
        view.setImageProcessingAsyncTask(null);
    }

    private RoundedBitmapDrawable getBitmapDrawable(Bitmap bitmap, Image image, ImageView view) {
        // Create a drawable to contain the bitmap.
        RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(view.getResources(), bitmap);
        rbd.setGravity(ImageScaleCalculator.getGravity(image.getAlign()));

        Dimension borderRadius = image.getBorderRadius();
        if (borderRadius != null) {
            rbd.setCornerRadius(borderRadius.value());
        }

        // Now that the bitmap has been loaded, we can draw the shadow bitmap
        if (image.hasShadow()) {
            image.setShouldDrawBoxShadow(true);
            image.setShadowBounds(rbd.getBitmap(), image.getAlign());
            ((View) view.getParent()).invalidate();
        }
        return rbd;
    }

    public static class BitmapRunnable implements Runnable {

        private final ImageViewAdapter mAdapter;
        private final APLImageView mView;
        private final Image mImage;
        private Bitmap mBitmap;
        private final boolean mSkipCache;
        private boolean mProcessingSuccessful;

        public void onSuccess(Bitmap bitmap) {
            mBitmap = bitmap;
            mProcessingSuccessful = true;
        }

        public void onFailure(Bitmap bitmap) {
            mBitmap = bitmap;
            mProcessingSuccessful = false;
        }

        public BitmapRunnable(ImageViewAdapter adapter, APLImageView view, Image image, boolean skipCache) {
            mAdapter = adapter;
            mView = view;
            mImage = image;
            mSkipCache = skipCache;
        }

        @Override
        public void run() {
            // Skip caching result if we're in error state
            mAdapter.setDrawableToImageView(mView, mImage, mBitmap, shouldSkipCache());
        }
        
        private boolean shouldSkipCache() {
            return mSkipCache || !mProcessingSuccessful;
        }
    }
}
