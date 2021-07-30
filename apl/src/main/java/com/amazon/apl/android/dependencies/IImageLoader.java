/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.amazon.apl.android.providers.ITelemetryProvider;

/**
 * ImageDownloader dependency.
 */
public interface IImageLoader {

    /**
     * Add bitmap success and fail metrics to this ImageLoader.
     * @param telemetry a metrics provider
     * @return this for chaining
     */
    default IImageLoader withTelemetry(ITelemetryProvider telemetry) {
        return this;
    }

    /**
     * Loads an image in asynchronous mode. Once the image has been fetched
     * from the source url, the callback will be invoked.
     *
     * @param path Image source to download.
     * @param imageView ImageView where sets the placerhold or error in place.
     * @param callback Notifies the result when the asynchronous thread has finished.
     *                 'onSuccess(..)' and 'onError(..)'. Either invoke will run onto
     *                 UI thread.
     */
    default void loadImage(final String path, ImageView imageView, LoadImageCallback2 callback, boolean needsScaling) {
        loadImage(path, imageView, new LoadImageCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                callback.onSuccess(bitmap, path);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception, path);
            }
        }, needsScaling);
    }

    /**
     * @deprecated Use {@link #loadImage(String, ImageView, LoadImageCallback2, boolean)}.
     *
     * Loads an image in asynchronous mode. Once the image has been fetched
     * from the source url, the callback will be invoked.
     *
     * @param path Image source to download.
     * @param imageView ImageView where sets the placerhold or error in place.
     * @param callback Notifies the result when the asynchronous thread has finished.
     *                 'onSuccess(..)' and 'onError(..)'. Either invoke will run onto
     *                 UI thread.
     */
    @Deprecated
    default void loadImage(String path, ImageView imageView, LoadImageCallback callback, boolean needsScaling) {}

    /**
     * @deprecated Use {@link #loadImage(String, ImageView, LoadImageCallback2, boolean)}.
     *
     * Loads an image in asynchronous mode. Once the image has been fetched
     * from the source url, the callback will be invoked.
     *
     * @param path Image source to download.
     * @param imageView ImageView where sets the placerhold or error in place.
     * @param callback Notifies the result when the asynchronous thread has finished.
     *                 'onSuccess(..)' and 'onError(..)'. Either invoke will run onto
     *                 UI thread.
     * @param options Properties which enables extra features.
     */
    @Deprecated
    default void loadImage(String path, ImageView imageView, LoadImageCallback callback, boolean needsScaling, ImageLoaderRequestOptions options) {}

    /**
     * Frees any resources such as bitmaps associated with the ImageView
     *
     * @param imageView the view that contains the resources
     */
    void clear(ImageView imageView);

    /**
     * Clear resources that are currently in use.
     */
    default void clearResources() {
        cancel();
    }

    /**
     *  Cancel the image download.
     */
    void cancel();

    /**
     * Class which holds ImageLoader optional and featured properties.
     */
    class ImageLoaderRequestOptions {
        private Drawable mPlaceholderDrawable;
        private Drawable mErrorDrawable;

        public void setPlaceholderDrawable(Drawable drawable) {
            mPlaceholderDrawable = drawable;
        }

        public void setErrorDrawable(Drawable drawable) {
            mErrorDrawable = drawable;
        }

        public Drawable getPlaceholderDrawable() {
            return mPlaceholderDrawable;
        }

        public Drawable getErrorDrawable() {
            return mErrorDrawable;
        }
    }

    /**
     * @deprecated Use {@link LoadImageCallback2} to support multisource images.
     * Callback for LoadImage method.
     */
    @Deprecated
    interface LoadImageCallback {
        void onSuccess(Bitmap bitmap);
        void onError(Exception exception);
    }

    interface LoadImageCallback2 {
        void onSuccess(Bitmap bitmap, String source);
        void onError(Exception exception, String source);
    }
}
