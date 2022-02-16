/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.ImageScale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LazyImageLoader {

    private static final String TAG = "LazyImageLoader";

    /**
     * Starts the image load.
     *
     * @param view The view to display the image.
     */
    public static void initImageLoad(ImageViewAdapter adapter, Image image, APLImageView view) {
        if (view.getDrawable() != null) {
            view.setImageDrawable(null);
            // Clear the resources for this view if we're loading a new bitmap
            LazyImageLoader.clearImageResources(adapter, image, view);
        }

        boolean needsScaling = (image.getScale() != ImageScale.kImageScaleNone);

        IImageLoader provider = image.getImageLoader(view.getContext());
        List<UrlRequests.UrlRequest> sources = image.getSourceRequests();
        if (sources.size() > 0) {
            ImageLoad loadImage = new ImageLoad(adapter, view, sources, provider, needsScaling);
            loadImage.load();
        }
    }

    /**
     * Clears any resources such as bitmaps associated with the ImageView
     *
     * @param image the Image component associated with the ImageView
     * @param imageView the ImageView to clear resources from
     */
    public static void clearImageResources(ImageViewAdapter imageViewAdapter, Image image, APLImageView imageView) {
        IImageLoader provider = image.getImageLoader(imageView.getContext());
        imageViewAdapter.clearAsyncTask(imageView);
        if(provider != null) {
            provider.clear(imageView);
        }
    }

    /**
     * Iteratively loads a list of {@link com.amazon.apl.android.primitive.UrlRequests.UrlRequest}
     * into an array of Bitmaps.
     */
    private static class ImageLoad {
        private final ImageViewAdapter mImageViewAdapter;
        private final APLImageView mImageView;
        private final IImageLoader mImageLoader;
        private final List<UrlRequests.UrlRequest> mSources = new ArrayList<>();
        private final Bitmap[] mBitmaps;
        private final boolean mNeedsScaling;

        ImageLoad(ImageViewAdapter imageViewAdapter, APLImageView imageView, List<UrlRequests.UrlRequest> sources, IImageLoader loader, boolean needsScaling) {
            mImageViewAdapter = imageViewAdapter;
            mImageView = imageView;
            mImageLoader = loader;
            mNeedsScaling = needsScaling;
            mBitmaps = new Bitmap[sources.size()];
            mSources.addAll(sources);
        }

        public void load() {
            for (int i = 0; i < mSources.size(); i++) {
                final int index = i;
                IImageLoader.LoadImageCallback2 callback = new LoadImageCallback(mImageView, (bitmap) -> {
                    mBitmaps[index] = bitmap;
                    boolean allLoaded = true;
                    for (Bitmap loaded : mBitmaps) {
                        if (loaded == null) {
                            allLoaded = false;
                            break;
                        }
                    }

                    if (allLoaded) {
                        mImageViewAdapter.onImageLoad(mImageView, Arrays.asList(mBitmaps));
                    }
                });

                IImageLoader.LoadImageParams load = IImageLoader.LoadImageParams.builder()
                        .path(mSources.get(index).url())
                        .imageView(mImageView)
                        .needsScaling(mNeedsScaling)
                        // TODO [ISSUE-22804] upscaling should never be allowed, but we need to preserve
                        //  existing behavior where both bitmaps are scaled to fill the target view
                        //  prior to Filters.
                        .allowUpscaling(mSources.size() > 1)
                        .headers(mSources.get(index).headers())
                        .callback(callback)
                        .build();
                mImageLoader.loadImage(load);
            }
        }
    }

    /**
     * Handles the callback from loading a single source, and notifies the APLViewPresenter
     * that the media has loaded, as well as returns the Bitmap result to the given {@link BitmapAcceptor}
     */
    private static class LoadImageCallback implements IImageLoader.LoadImageCallback2 {
        private final APLImageView mImageView;
        private final BitmapAcceptor mResult;
        private boolean mHasReturned = false;

        LoadImageCallback(APLImageView imageView, BitmapAcceptor result) {
            mImageView = imageView;
            mResult = result;
        }

        @Override
        public synchronized void onSuccess(Bitmap bitmap, String source) {
            mImageView.getPresenter().mediaLoaded(source);
            setBitmap(bitmap);
        }

        @Override
        public synchronized void onError(Exception exception, String source) {
            onError(exception, 0, source);
        }

        @Override
        public synchronized void onError(Exception exception, int errorCode, String source) {
            String errorMessage = "";
            if (exception != null) {
                errorMessage = exception.getMessage();
            }
            mImageView.getPresenter().mediaLoadFailed(source, errorCode, errorMessage);
            Log.e(TAG, "error loading image", exception);
            renderErrorBitmap(source);
        }

        private synchronized void renderErrorBitmap(String source) {
            // Call onImageLoad call with a black image as per the spec:
            Bitmap errorBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            this.setBitmap(errorBitmap);
        }

        private synchronized void setBitmap(Bitmap bitmap) {
            // Callbacks should not be invoked more than once
            if (mHasReturned) return;
            this.mResult.accept(bitmap);
            mHasReturned = true;
        }

        interface BitmapAcceptor {
            void accept(Bitmap bitmap);
        }
    }
}