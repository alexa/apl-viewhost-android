/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.sgcontent.Node;
import com.google.auto.value.AutoValue;

import java.io.File;
import java.util.Map;

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
     * @param load The image load parameters
     */
    default void loadImage(LoadImageParams load) {
        loadImage(load.path(), load.imageView(), load.callback(), load.needsScaling());
    }

    void downloadImage(DownloadImageParams load);

    /**
     * @deprecated Use {@link #loadImage(LoadImageParams)}.
     */
    @Deprecated
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
    default void cancel() {}

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

        /**
         * Adds an additional errorCode that will be passed on to image onFail callbacks.
         * @param exception The exception that occurred
         * @param errorCode An error code as defined by the runtime, such as the HttpResponseCode.
         * @param source The url of the image source that failed to load
         */
        default void onError(Exception exception, int errorCode, String source) {
            onError(exception, source);
        }
    }

    interface DownloadImageCallback {
        void onSuccess(File file, String source);

        /**
         * Called when there is an error downloading the requested image.
         * @param exception The exception that occurred
         * @param errorCode An error code as defined by the runtime, such as the HttpResponseCode.
         * @param source The url of the image source that failed to download
         */
        void onError(Exception exception, int errorCode, String source);
    }

    /**
     * Parameters for an Image download.
     */
    @AutoValue
    abstract class DownloadImageParams {
        /**
         * @return the path to the image
         */
        public abstract String path();

        /**
         * @return the callback for completion or error
         */
        public abstract DownloadImageCallback callback();

        /**
         * @return the request headers.
         */
        public abstract Map<String, String> headers();


        public static Builder builder() {
            return new AutoValue_IImageLoader_DownloadImageParams.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder path(String path);
            public abstract Builder callback(DownloadImageCallback callbacks);
            public abstract Builder headers(Map<String, String> headers);
            public abstract DownloadImageParams build();
        }
    }

    /**
     * Parameters for an Image load.
     */
    @AutoValue
    abstract class LoadImageParams {
        /**
         * @return the path to the image
         */
        public abstract String path();

        /**
         * @return the target imageview
         */
        public abstract ImageView imageView();

        /**
         * @return the callback for completion or error
         */
        public abstract LoadImageCallback2 callback();

        /**
         * @return whether the bitmap should be scaled to fit the target view.
         *      For example, {@link com.amazon.apl.enums.ImageScale#kImageScaleNone} would return false.
         */
        public abstract boolean needsScaling();

        /**
         * @return the request headers.
         */
        public abstract Map<String, String> headers();

        /**
         * @return whether the bitmap should be scaled up to fill the target view. This is false unless
         *      we are have multiple sources to blend to maintain a previous quirk.
         */
        public abstract boolean allowUpscaling();

        public static Builder builder() {
            return new AutoValue_IImageLoader_LoadImageParams.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder path(String path);
            public abstract Builder imageView(ImageView imageView);
            public abstract Builder callback(LoadImageCallback2 callbacks);
            public abstract Builder needsScaling(boolean needsScaling);
            public abstract Builder headers(Map<String, String> headers);
            public abstract Builder allowUpscaling(boolean allowUpscaling);
            public abstract LoadImageParams build();
        }
    }
}
