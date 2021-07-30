/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.widget.ImageView;

import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of IImageLoader interface.
 * <p>
 * It uses Glide as third-party, Glide is a fast and efficient image loading library for
 * Android focused on smooth scrolling.
 * <p>
 * See more: https://bumptech.github.io/glide/
 */
public class GlideImageLoader implements IImageLoader {
    private static final String TAG = "GlideImageLoader";

    static final String METRIC_IMAGE_SUCCESS  = TAG + ".loadImage.success";
    static final String METRIC_IMAGE_FAIL = TAG + ".loadImage.fail";

    private final RequestManager mGlideClient;
    private final Glide mGlide;
    private final Map<ImageView, Target<?>> mTargets = new ConcurrentHashMap<>();
    private final Map<ImageView, Target<?>> mPendingTargets = new ConcurrentHashMap<>();
    private ITelemetryProvider mTelemetryProvider;

    private int cImageSuccess = ITelemetryProvider.UNKNOWN_METRIC_ID;
    private int cImageFail = ITelemetryProvider.UNKNOWN_METRIC_ID;

    /**
     * GlideImageDownloader constructor.
     */
    public GlideImageLoader(@NonNull final Context context) {
        mGlideClient = Glide.with(context);
        mGlide = Glide.get(context);
    }

    @Override
    public GlideImageLoader withTelemetry(ITelemetryProvider telemetryProvider) {
        // Set only as many times as is needed
        if (mTelemetryProvider == null || !mTelemetryProvider.equals(telemetryProvider)) {
            mTelemetryProvider = telemetryProvider;
            cImageSuccess = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_IMAGE_SUCCESS, ITelemetryProvider.Type.COUNTER);
            cImageFail = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_IMAGE_FAIL, ITelemetryProvider.Type.COUNTER);
        }
        return this;
    }

    @Override
    public void loadImage(@NonNull String path, @NonNull ImageView imageView,
                          @NonNull LoadImageCallback2 callback, boolean needsScaling) {
        loadImageInternal(path, imageView, callback, needsScaling, new ImageLoaderRequestOptions(), new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888));
    }

    private void loadImageInternal(@NonNull String path, @NonNull ImageView imageView,
                           @NonNull LoadImageCallback2 callback, boolean needsScaling,
                           @NonNull ImageLoaderRequestOptions options,
                           @NonNull RequestOptions requestOptions) {
        Drawable placeholderDrawable = options.getPlaceholderDrawable();
        Drawable errorDrawable = options.getErrorDrawable();

        BitmapImageViewTargetWithCallback target = new BitmapImageViewTargetWithCallback(path, imageView,
                callback, this, needsScaling);
        mTargets.put(imageView, target);
        mPendingTargets.put(imageView, target);

        if (placeholderDrawable != null) {
            requestOptions = requestOptions.placeholder(placeholderDrawable);
        }

        if (errorDrawable != null) {
            requestOptions = requestOptions.error(errorDrawable);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestOptions = requestOptions.disallowHardwareConfig();
        }

        // TODO we should be able to use a combination of Filter signature and path to
        //  cache the processed bitmap in Glide's cache and avoid reprocessing the image
        //  requestOptions = requestOptions.signature(new ObjectKey(image uuid, filters));
        RequestBuilder<Bitmap> requestBuilder = mGlideClient
                .setDefaultRequestOptions(requestOptions)
                .asBitmap()
                .load(path)
                .listener(target);

        requestBuilder.into(target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(ImageView imageView) {
        mGlideClient.clear(mTargets.get(imageView));
        mTargets.remove(imageView);
        mPendingTargets.remove(imageView);
    }

    /**
     * Cancel pending image requests.
     *
     * TODO provide caching/recycling mechanism for bitmaps and avoid needing to cancel pending
     *  requests.
     */
    @VisibleForTesting
    void cancelPending() {
        for (ImageView imageView : mPendingTargets.keySet()) {
            if (mTelemetryProvider != null) {
                mTelemetryProvider.incrementCount(cImageFail);
            }
            clear(imageView);
        }
        mPendingTargets.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void clearResources() {
        cancelPending();
        for(ImageView imageView : mTargets.keySet()) {
            clear(imageView);
        }

        mGlide.clearMemory();
    }

    public void cancel() {
        // unused
    }

    /**
     * Remove a pending target from the set when finished.
     * @param view the target imageview to remove.
     */
    private void removePendingTarget(ImageView view) {
        mPendingTargets.remove(view);
    }

    @VisibleForTesting
    Map<ImageView, Target<?>> getTargets() {
        return mTargets;
    }

    @VisibleForTesting
    Map<ImageView, Target<?>> getPendingTargets() {
        return mPendingTargets;
    }

    /**
     * BitmapSimple target that provides a Bitmap to the callback.  Glide manages a cache and download retries.
     */
    static class BitmapImageViewTargetWithCallback extends BitmapImageViewTarget implements RequestListener<Bitmap> {
        private final String mPath;
        @NonNull
        private final LoadImageCallback2 mCallback;
        private final boolean mNeedsScaling;
        private final GlideImageLoader mGlideImageLoader;

        BitmapImageViewTargetWithCallback(String path, @NonNull ImageView imageView, @NonNull LoadImageCallback2 callback, GlideImageLoader glideImageLoader,
                                          boolean needsScaling) {
            super(imageView);
            mPath = path;
            mCallback = callback;
            mNeedsScaling = needsScaling;
            mGlideImageLoader = glideImageLoader;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
            mGlideImageLoader.removePendingTarget(getView());
            if (mGlideImageLoader.mTelemetryProvider != null) {
                mGlideImageLoader.mTelemetryProvider.incrementCount(mGlideImageLoader.cImageFail);
            }

            mCallback.onError(e, mPath);
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, @NonNull Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }

        @Override
        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
            if (mGlideImageLoader.mTelemetryProvider != null) {
                mGlideImageLoader.mTelemetryProvider.incrementCount(mGlideImageLoader.cImageSuccess);
            }

            mGlideImageLoader.removePendingTarget(getView());
            mCallback.onSuccess(resource, mPath);
        }

        /**
         * Defines the size of the bitmap to download.
         * <p>
         * NOTE: When a bitmap is downloaded, Glide will resize the bitmap to fit into the view where will be displayed in.
         * Here explains the default behavior: https://bumptech.github.io/glide/doc/targets.html#sizes-and-dimensions
         * <p>
         * However, there is a case when we do not need to resize the bitmap, especially when:
         * - the image size is smaller than view size and
         * - no scaling is needed.
         * <p>
         * For example, let's say a bitmap size is 100x100 px and a view size is 500x500 px. Glide (by default) would resize the bitmap to 500x500 px.
         * This is unacceptable behavior when "scale"="none" is defined in the component.
         * <p>
         * To override this, we require to call:
         * `cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);`
         *
         * @param cb Callback to tell the final size of the bitmap.
         */
        @Override
        public void getSize(@NonNull SizeReadyCallback cb) {
            if (!mNeedsScaling) {
                // TODO large bitmaps can fail to load (or generally take up too much memory)
                //  when using original size.
                cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
            }
            super.getSize(cb);
        }
    }
}
