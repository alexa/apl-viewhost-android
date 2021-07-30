/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.listeners.OnImageAttachStateChangeListener;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.ImageScale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LazyImageLoader {

    private static final String TAG = "LazyImageLoader";
    /**
     * The method triggers the load of the image when the view becomes part of the view tree.
     *
     * @param view The view to display the image.
     */
    public static void lazyLoadImage(ImageViewAdapter adapter, Image image, APLImageView view) {
        if (view.isAttachedToWindow()) {
            initImageLoad(adapter, image, view);
        } else {
            view.setOnImageAttachStateChangeListener(new OnImageAttachStateChangeListener(adapter, image, view));
        }
    }

    /**
     * Starts the image load.
     *
     * @param view The view to display the image.
     */
    public static void initImageLoad(ImageViewAdapter adapter, Image image, APLImageView view) {
        boolean needsScaling = (image.getScale() != ImageScale.kImageScaleNone);

        IImageLoader provider = image.getImageLoader(view.getContext());
        List<String> sources = image.getSources();
        if (sources.size() > 0) {
            // TODO
            //  Provide generic Targets instead of ViewTargets to enable simultaneous loading of source URLs
            //  Glide internally uses the View to clear loads if called more than once before a load finishes.
            //  See https://github.com/bumptech/glide/issues/514
            LoadImage loadImage = new LoadImage(adapter, view, sources, provider, needsScaling);
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
        imageViewAdapter.clearAsyncTask(true, imageView);
        if(provider != null) {
            provider.clear(imageView);
        }
    }

    /**
     * Loads an image into the array of bitmaps.
     */
    private static class LoadImage implements IImageLoader.LoadImageCallback2 {
        private final ImageViewAdapter mImageViewAdapter;
        private final APLImageView mImageView;
        private final IImageLoader mImageLoader;
        private final List<String> mSources = new ArrayList<>();
        private final List<String> mPendingSources = new ArrayList<>();
        private final Bitmap[] mBitmaps;
        private final boolean mNeedsScaling;
        private boolean mSkipCache = false;

        LoadImage(ImageViewAdapter imageViewAdapter, APLImageView imageView, List<String> sources, IImageLoader loader, boolean needsScaling) {
            mImageViewAdapter = imageViewAdapter;
            mImageView = imageView;
            mImageLoader = loader;
            mNeedsScaling = needsScaling;
            mSources.addAll(sources);
            mPendingSources.addAll(sources);
            mBitmaps = new Bitmap[sources.size()];
        }

        /**
         * Starts the image load.
         */
        public void load() {
            mImageLoader.loadImage(mSources.get(0), mImageView, this, mNeedsScaling);
        }

        @Override
        public synchronized void onSuccess(Bitmap bitmap, String source) {
            mPendingSources.remove(source);
            mBitmaps[mSources.indexOf(source)] = bitmap;
            if (mPendingSources.isEmpty()) {
                mImageViewAdapter.onImageLoad(mImageView, Arrays.asList(mBitmaps), mSkipCache);
            } else {
                final String nextSource = mPendingSources.get(0);
                // TODO
                //  Provide generic Targets instead of ViewTargets to enable simultaneous loading of source URLs
                //  Glide internally uses the View to clear loads if called more than once before a load finishes.
                //  See https://github.com/bumptech/glide/issues/514
                // Load next source
                Handler handler = new Handler(Looper.myLooper());
                handler.post(() -> mImageLoader.loadImage(nextSource, mImageView, this, mNeedsScaling));
            }
        }

        @Override
        public synchronized void onError(Exception exception, String source) {
            // Call onImageLoad call with a black image as per the spec:
            Log.e(TAG, "error loading image", exception);
            mSkipCache = true;
            Bitmap errorBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            onSuccess(errorBitmap, source);
        }
    }
}