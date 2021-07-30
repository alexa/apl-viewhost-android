/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.views.APLImageView;
import com.bumptech.glide.load.engine.GlideException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlideImageLoaderTest extends ViewhostRobolectricTest {
    @Mock
    private IImageLoader.LoadImageCallback2 mCallback;
    @Mock
    private ITelemetryProvider mTelemetry;
    @Mock
    private IAPLViewPresenter mockPresenter;
    
    private ImageView mImageView;
    private GlideImageLoader mImageLoader;
    private GlideImageLoader.BitmapImageViewTargetWithCallback mTarget;

    private static final String URL = "https://via.placeholder.com/300";
    private static final boolean NEEDS_SCALING = true;

    private static final int cImageSuccess = 1;
    private static final int cImageFail = 2;

    @Before
    public void setup() {
        // Prepare mock items
        mImageView = new APLImageView(ViewhostRobolectricTest.getApplication().getApplicationContext(), mockPresenter);
        mCallback = mock(IImageLoader.LoadImageCallback2.class);
        when(mTelemetry.createMetricId(anyString(), eq(GlideImageLoader.METRIC_IMAGE_SUCCESS), eq(ITelemetryProvider.Type.COUNTER))).thenReturn(cImageSuccess);
        when(mTelemetry.createMetricId(anyString(), eq(GlideImageLoader.METRIC_IMAGE_FAIL), eq(ITelemetryProvider.Type.COUNTER))).thenReturn(cImageFail);

        mImageLoader = new GlideImageLoader(ViewhostRobolectricTest.getApplication().getApplicationContext());
        mImageLoader.withTelemetry(mTelemetry);
        mTarget = new GlideImageLoader.BitmapImageViewTargetWithCallback(URL, mImageView, mCallback, mImageLoader, NEEDS_SCALING);
    }

    @Test
    public void loadImage_setsTargets() {
        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        GlideImageLoader.BitmapImageViewTargetWithCallback target = (GlideImageLoader.BitmapImageViewTargetWithCallback) mImageLoader.getTargets().get(mImageView);
        assertNotNull(target);
        assertEquals(mImageLoader.getTargets(), mImageLoader.getPendingTargets());
        assertEquals(1, mImageLoader.getTargets().size());
        assertEquals(1, mImageLoader.getPendingTargets().size());
    }

    @Test
    public void loadImage_cancelPending() {
        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        mImageLoader.cancelPending();

        assertEquals(Collections.EMPTY_MAP, mImageLoader.getTargets());
        assertEquals(Collections.EMPTY_MAP, mImageLoader.getPendingTargets());
        verify(mTelemetry).incrementCount(cImageFail);
    }

    @Test
    public void loadImage_clearResources() {
        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        mImageLoader.clearResources();

        assertEquals(Collections.EMPTY_MAP, mImageLoader.getTargets());
        assertEquals(Collections.EMPTY_MAP, mImageLoader.getPendingTargets());
    }

    @Test
    public void clearImage() {
        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        mImageLoader.clear(mImageView);

        assertEquals(Collections.EMPTY_MAP, mImageLoader.getTargets());
        assertEquals(Collections.EMPTY_MAP, mImageLoader.getPendingTargets());
    }

    @Test
    public void loadImageCallback_InvokeOnSuccessCase() {
        mImageLoader.loadImage(URL, mImageView, mCallback, NEEDS_SCALING);

        Bitmap bitmap = createDummyBitmap();
        GlideImageLoader.BitmapImageViewTargetWithCallback target = (GlideImageLoader.BitmapImageViewTargetWithCallback) mImageLoader.getTargets().get(mImageView);
        target.onResourceReady(bitmap, null);

        assertEquals(Collections.EMPTY_MAP, mImageLoader.getPendingTargets());
        verify(mCallback).onSuccess(eq(bitmap), eq(URL));
        verify(mTelemetry).incrementCount(cImageSuccess);
    }

    @Test
    public void loadImageCallback_InvokeOnErrorCase() {
        GlideException glideException = new GlideException("Network problem", new IOException("No connectivity to server"));
        mTarget.onLoadFailed(glideException, URL, mTarget, NEEDS_SCALING);

        verify(mCallback).onError(eq(glideException), eq(URL));
        verify(mTelemetry).incrementCount(cImageFail);
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
