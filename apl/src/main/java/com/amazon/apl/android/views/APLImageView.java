/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import android.widget.ImageView;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.image.ImageProcessingAsyncTask;
import com.amazon.apl.android.listeners.OnImageAttachStateChangeListener;
import com.amazon.apl.android.utils.LazyImageLoader;

@SuppressLint("AppCompatCustomView")
public class APLImageView extends ImageView {

    private IAPLViewPresenter mPresenter;
    private OnImageAttachStateChangeListener mOnImageAttachStateChangeListener;
    private AsyncTask mImageProcessingAsyncTask;

    public APLImageView(Context context, final IAPLViewPresenter presenter) {
        super(context);
        mPresenter = presenter;
        setScaleType(ScaleType.FIT_XY);
    }

    public IAPLViewPresenter getPresenter() {
        return mPresenter;
    }

    @Nullable
    public AsyncTask getImageProcessingAsyncTask() {
        return mImageProcessingAsyncTask;
    }

    public void setImageProcessingAsyncTask(ImageProcessingAsyncTask task) {
        mImageProcessingAsyncTask = task;
    }

    /**
     * Removes the previously set listener and adds the given listener to the view
     *
     * @param onImageAttachStateChangeListener the event listener
     */
    public void setOnImageAttachStateChangeListener(OnImageAttachStateChangeListener onImageAttachStateChangeListener) {
        if (mOnImageAttachStateChangeListener != null) {
            removeOnAttachStateChangeListener(mOnImageAttachStateChangeListener);
        }
        mOnImageAttachStateChangeListener = onImageAttachStateChangeListener;
        addOnAttachStateChangeListener(mOnImageAttachStateChangeListener);
    }

    public OnImageAttachStateChangeListener getOnImageAttachStateChangeListener() {
        return mOnImageAttachStateChangeListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LazyImageLoader.clearImageResources(ImageViewAdapter.getInstance(), (Image) mPresenter.findComponent(this), this);
    }
}
