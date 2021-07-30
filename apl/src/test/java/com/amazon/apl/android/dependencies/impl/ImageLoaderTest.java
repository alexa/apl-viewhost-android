/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ImageLoaderTest extends ViewhostRobolectricTest {

    private ImageView mImageView;
    private ArgumentCaptor<IImageLoader.LoadImageCallback> mCallbackCaptor;
    private IImageLoader.LoadImageCallback mCallback;
    private IImageLoader mImageLoader;

    private static final String URL = "Any url";
    private static final boolean NEEDS_SCALING = true;

    @Before
    public void setup() {
        mImageLoader = mock(IImageLoader.class);
        mImageView = mock(ImageView.class);
        mCallbackCaptor = ArgumentCaptor.forClass(IImageLoader.LoadImageCallback.class);
        mCallback = mock(IImageLoader.LoadImageCallback.class);
    }

    @Test
    public void loadImage_VerifyOnSucessIsCalledCase() {

        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        verify(mImageLoader).loadImage(eq(URL), eq(mImageView), mCallbackCaptor.capture(), eq(NEEDS_SCALING));

        // assertion about the state before the callback is called
        verify(mCallback, never()).onSuccess(any(Bitmap.class));

        // trigger the reply on callbackCaptor.getValue()
        mCallbackCaptor.getValue().onSuccess(createDummyBitmap());

        // assertion about the state after the callback is called
        verify(mCallback).onSuccess(any(Bitmap.class));
    }

    @Test
    public void loadImage_VerifyOnErrorIsCalledCase() {
        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        verify(mImageLoader).loadImage(eq(URL), eq(mImageView), mCallbackCaptor.capture(), eq(NEEDS_SCALING));

        // assertion about the state before the callback is called
        verify(mCallback, never()).onError(any(Exception.class));

        // trigger the reply on callbackCaptor.getValue()
        mCallbackCaptor.getValue().onError(new IOException("Malformed url"));

        // assertion about the state after the callback is called
        verify(mCallback).onError(any(Exception.class));
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
