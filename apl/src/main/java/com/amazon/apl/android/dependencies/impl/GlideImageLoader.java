/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.sgcontent.Node;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

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
    /**
     * Any valid scheme return by {@link Uri#getScheme()} that specifies a HTTP request.
     * null represents an HTTP URL without any scheme specified, which is supported in APL <= 1.1
     */
    private static final Set<String> VALID_URL_SCHEMES = new HashSet<>(Arrays.asList("https", "http", null));

    static final String METRIC_IMAGE_SUCCESS  = TAG + ".loadImage.success";
    static final String METRIC_IMAGE_FAIL = TAG + ".loadImage.fail";

    private final Map<ImageView, List<Target<?>>> mTargets = new HashMap<>();
    private final Map<Node, List<Target<?>>> mNodeTargets = new HashMap<>();
    private ITelemetryProvider mTelemetryProvider;

    private int cImageSuccess = ITelemetryProvider.UNKNOWN_METRIC_ID;
    private int cImageFail = ITelemetryProvider.UNKNOWN_METRIC_ID;

    private static final NoUpscalingDownsampleStrategy DOWNSAMPLE_STRATEGY = new NoUpscalingDownsampleStrategy();

    private final Context mContext;

    /**
     * GlideImageDownloader constructor.
     */
    public GlideImageLoader(@NonNull final Context context) {
        mContext = context.getApplicationContext();
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
    public void loadImage(LoadImageParams load) {
        if (load != null) loadImageInternal(load, Glide.with(mContext));
    }

    private RequestOptions buildLoadImageRequestOptions(Map<String, String> headers, boolean allowUpscaling) {
        RequestOptions requestOptions = new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888);
        ImageLoaderRequestOptions options = new ImageLoaderRequestOptions();
        Drawable placeholderDrawable = options.getPlaceholderDrawable();
        Drawable errorDrawable = options.getErrorDrawable();

        if (placeholderDrawable != null) {
            requestOptions = requestOptions.placeholder(placeholderDrawable);
        }

        if (errorDrawable != null) {
            requestOptions = requestOptions.error(errorDrawable);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestOptions = requestOptions.disallowHardwareConfig();
        }

        // This is the same behavior as the default downsample strategy we were using previously
        // CenterOutside except it doesn't allow for the bitmap to be upscaled.
        // This makes sense for us because we will scale the bitmap using a Matrix in our draw
        // phase. There is a functional discrepancy with image filters when the source/dest bitmaps are not
        // the same aspect ratio. Previously we would've scaled both bitmaps to fill the target view
        // and then applied filters. This isn't correct per the apl spec, but we will quirk this behavior
        // to older document versions and capture metrics around it.
        if (!allowUpscaling) {
            requestOptions = requestOptions.downsample(DOWNSAMPLE_STRATEGY);
        }

        if (headers.size() > 0) {
            requestOptions = requestOptions.signature(new ObjectKey(headers));
        }

        return requestOptions;
    }

    private RequestOptions buildDownloadOnlyRequestOptions(Map<String, String> headers) {
        RequestOptions requestOptions = new RequestOptions();

        if (headers.size() > 0) {
            requestOptions = requestOptions.signature(new ObjectKey(headers));
        }

        return requestOptions;
    }

    @Override
    public void downloadImage(DownloadImageParams downloadImageParams) {
            Glide.with(mContext)
                .setDefaultRequestOptions(buildLoadImageRequestOptions(downloadImageParams.headers(), false))
                .downloadOnly()
                // override default downloadOnly priority of low since this required to resolve
                // the image dimensions of images we are trying to display now
                .priority(Priority.NORMAL)
                .addListener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        downloadImageParams.callback().onError(e, parseErrorCodeFromException(e), downloadImageParams.path());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        downloadImageParams.callback().onSuccess(resource, downloadImageParams.path());
                        return false;
                    }
                })
                .load(downloadImageParams.path())
                .submit();
    }

    @VisibleForTesting
    void loadImageInternal(@NonNull LoadImageParams load, @NonNull RequestManager requestManager) {
        final ImageView imageView = load.imageView();
        final boolean needsScaling = load.needsScaling();
        final IImageLoader.LoadImageCallback2 callback = load.callback();
        final String url = load.path();
        final boolean allowUpscaling = load.allowUpscaling();
        final Map<String, String> headers = load.headers();

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        if (layoutParams == null) {
            Log.wtf(TAG, "Attempting to load image for image view with no layout params.");
            return;
        }

        // Get original size if we are not scaling
        int targetWidth = needsScaling ? layoutParams.width : SimpleTarget.SIZE_ORIGINAL;
        int targetHeight = needsScaling ? layoutParams.height : SimpleTarget.SIZE_ORIGINAL;
        BitmapTarget target = createTarget(requestManager, imageView, callback, url, targetWidth, targetHeight);

        // Resume requests if Glide client being paused by other app (eg. mmsdk)
        if (requestManager.isPaused()) {
            requestManager.resumeRequests();
        }

        try {
            RequestBuilder<Bitmap> requestBuilder = requestManager
                    .setDefaultRequestOptions(buildLoadImageRequestOptions(headers, allowUpscaling))
                    .asBitmap();

            // GlideUrl class only works for loading HTTP urls
            if (isUrlRequest(url) && headers.size() > 0) {
                requestBuilder = requestBuilder
                        .load(new GlideUrl(url, new GlideStaticHeaders(headers)));
            } else {
                requestBuilder = requestBuilder
                        .load(url);
            }

            requestBuilder
                    .listener(target)
                    .into(target);
        } catch (final RejectedExecutionException e) {
            Log.wtf(TAG, "Glide failed to load image: " + e);
            if (mTelemetryProvider != null) {
                mTelemetryProvider.incrementCount(cImageFail);
            }
            load.callback().onError(e, load.path());
        }
    }

    /**
     * Determines if this URI is an http/https url by parsing the scheme. If no
     * scheme is provided, it is assumed this will be a url request.
     * @param path The URI to get the scheme from
     * @return True if the path is an http/https url, or if no scheme is provided
     */
    private boolean isUrlRequest(String path) {
        String scheme = Uri.parse(path).getScheme();
        if (scheme == null) {
            return true;
        }
        return VALID_URL_SCHEMES.contains(scheme.toLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear(ImageView imageView) {
        List<Target<?>> targets = mTargets.get(imageView);
        if (targets != null) {
            for (Target<?> target : targets) {
                RequestManager requestManager = ((BitmapTarget) target).getRequestManager();
                requestManager.clear(target);
            }
        }
        mTargets.remove(imageView);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clearResources() {
        for (Map.Entry<ImageView, List<Target<?>>> entry : mTargets.entrySet()) {
            List<Target<?>> targets = entry.getValue();
            if (targets != null) {
                for (Target<?> target : targets) {
                    RequestManager requestManager = ((BitmapTarget) target).getRequestManager();
                    requestManager.clear(target);
                }
            }
        }
        mTargets.clear();
    }

    @VisibleForTesting
    Map<ImageView, List<Target<?>>> getTargets() {
        return mTargets;
    }

    /**
     * Does a best attempt to get a status/error code from the glide exception,
     * however glide exceptions do not easily expose the status.
     * @param glideException
     * @return
     */
    private int parseErrorCodeFromException(@Nullable GlideException glideException) {
        int errorCode = 0;
        if (glideException == null) return errorCode;
        // Parse the HttpStatusCode, if one exists
        for (Throwable cause : glideException.getRootCauses()) {
            if (cause instanceof HttpException) {
                return ((HttpException) cause).getStatusCode();
            } else if (cause instanceof FileNotFoundException) {
                return HttpURLConnection.HTTP_NOT_FOUND;
            } else if (cause instanceof SocketTimeoutException) {
                return HttpURLConnection.HTTP_CLIENT_TIMEOUT;
            }
        }
        return errorCode;
    }

    /**
     * Creates a target for Glide to load a Bitmap into. It is expected that you eventually call
     * {@link #clear(ImageView)} on the ImageView you supply to avoid memory leaks in Glide.
     *
     * @param imageView the ImageView that this bitmap is going to load into.
     * @param callback  the callback for finishing the load
     * @param url       the path to the source
     * @param width     the width of the target
     * @param height    the height of the target
     * @param requestManager    requestManager reference for cleanup
     * @return          a BitmapTarget.
     */
    @VisibleForTesting
    synchronized BitmapTarget createTarget(@NonNull RequestManager requestManager, @NonNull ImageView imageView, @NonNull LoadImageCallback2 callback, @NonNull String url, int width, int height) {
        BitmapTarget target = new BitmapTarget(requestManager, callback, url, width, height);
        List<Target<?>> targets = mTargets.get(imageView);
        if (targets == null) {
            targets = new LinkedList<>();
            if (!mTargets.containsKey(imageView)) {
                mTargets.put(imageView, targets);
            }
        }
        targets.add(target);
        return target;
    }

    /**
     * A {@link SimpleTarget} that loads a bitmap with the requested width and height according
     * to the {@link DownsampleStrategy}. Glide internally won't clear the resource for this target
     * so we have to do so manually in {@link #clear(ImageView)} or {@link #clearResources()}.
     */
    class BitmapTarget extends SimpleTarget<Bitmap> implements RequestListener<Bitmap> {
        @NonNull
        private final LoadImageCallback2 mCallback;
        @NonNull
        private final String mUrl;
        @NonNull
        private final RequestManager mRequestManager;

        BitmapTarget(@NonNull RequestManager requestManager, @NonNull LoadImageCallback2 callback, @NonNull String url, int width, int height) {
            super(width, height);
            mRequestManager = requestManager;
            mCallback = callback;
            mUrl = url;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
            if (mTelemetryProvider != null) {
                mTelemetryProvider.incrementCount(cImageFail);
            }
            int errorCode = parseErrorCodeFromException(e);

            mCallback.onError(e, errorCode, mUrl);
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }

        @Override
        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
            if (mTelemetryProvider != null) {
                mTelemetryProvider.incrementCount(cImageSuccess);
            }

            mCallback.onSuccess(resource, mUrl);
        }

        @NonNull
        RequestManager getRequestManager() {
            return mRequestManager;
        }
    }

    /**
     * A {@link DownsampleStrategy} that is the same as the default strategy {@link DownsampleStrategy#CENTER_OUTSIDE}
     * except that it doesn't allow the bitmap to be scaled up. This avoids allocating potentially large
     * bitmaps if the target view is large enough.
     */
    static class NoUpscalingDownsampleStrategy extends DownsampleStrategy {
        @Override
        public float getScaleFactor(int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
            float widthPercentage = requestedWidth / (float) sourceWidth;
            float heightPercentage = requestedHeight / (float) sourceHeight;
            return Math.min(1f, Math.max(widthPercentage, heightPercentage));
        }

        @Override
        public SampleSizeRounding getSampleSizeRounding(int sourceWidth, int sourceHeight, int requestedWidth, int requestedHeight) {
            return SampleSizeRounding.QUALITY;
        }
    }
}
