/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.views.APLImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlideImageLoaderTest extends ViewhostRobolectricTest {
    @Mock
    private IImageLoader.LoadImageCallback2 mCallback;
    @Mock
    private ITelemetryProvider mTelemetry;
    @Mock
    private IAPLViewPresenter mockPresenter;
    @Mock
    private RequestManager mRequestManager;
    
    private ImageView mImageView;
    private GlideImageLoader mImageLoader;

    private static final String URL = "https://via.placeholder.com/300";
    private static final boolean NEEDS_SCALING = true;

    private static final int cImageSuccess = 1;
    private static final int cImageFail = 2;

    @Before
    public void setup() {
        // Prepare mock items
        Context applicationContext = ViewhostRobolectricTest.getApplication().getApplicationContext();
        mImageView = new APLImageView(applicationContext, mockPresenter);
        mImageView.setLayoutParams(new APLAbsoluteLayout.LayoutParams(100, 100, 0, 0));
        mCallback = mock(IImageLoader.LoadImageCallback2.class);
        when(mTelemetry.createMetricId(anyString(), eq(GlideImageLoader.METRIC_IMAGE_SUCCESS), eq(ITelemetryProvider.Type.COUNTER))).thenReturn(cImageSuccess);
        when(mTelemetry.createMetricId(anyString(), eq(GlideImageLoader.METRIC_IMAGE_FAIL), eq(ITelemetryProvider.Type.COUNTER))).thenReturn(cImageFail);

        mImageLoader = new GlideImageLoader(applicationContext);
        mImageLoader.withTelemetry(mTelemetry);
    }

    @Test
    public void loadImage_setsTargets() {
        mImageLoader.loadImage(
                IImageLoader.LoadImageParams.builder()
                        .path(URL)
                        .imageView(mImageView)
                        .callback(mCallback)
                        .needsScaling(NEEDS_SCALING)
                        .headers(Collections.emptyMap())
                        .allowUpscaling(false)
                        .build());

        GlideImageLoader.BitmapTarget target = (GlideImageLoader.BitmapTarget) mImageLoader.getTargets().get(mImageView).get(0);
        assertNotNull(target);
        assertEquals(1, mImageLoader.getTargets().size());
    }

    @Test
    public void loadImage_clearResources() {
        mImageLoader.loadImage(
                IImageLoader.LoadImageParams.builder()
                        .path(URL)
                        .imageView(mImageView)
                        .callback(mCallback)
                        .needsScaling(NEEDS_SCALING)
                        .headers(Collections.emptyMap())
                        .allowUpscaling(false)
                        .build());

        mImageLoader.clearResources();

        assertEquals(Collections.EMPTY_MAP, mImageLoader.getTargets());
    }

    @Test
    public void clearImage() {
        // given
        mImageLoader.createTarget(mRequestManager, mImageView, mCallback, URL, 100, 100);

        // when
        mImageLoader.clear(mImageView);

        // verify
        assertEquals(Collections.EMPTY_MAP, mImageLoader.getTargets());
    }

    @Test
    public void loadImageCallback_InvokeOnSuccessCase() {
        mImageLoader.loadImage(
                IImageLoader.LoadImageParams.builder()
                        .path(URL)
                        .imageView(mImageView)
                        .callback(mCallback)
                        .needsScaling(NEEDS_SCALING)
                        .headers(Collections.emptyMap())
                        .allowUpscaling(false)
                        .build());

        Bitmap bitmap = createDummyBitmap();
        GlideImageLoader.BitmapTarget target = (GlideImageLoader.BitmapTarget) mImageLoader.getTargets().get(mImageView).get(0);
        target.onResourceReady(bitmap, null);

        verify(mCallback).onSuccess(eq(bitmap), eq(URL));
        verify(mTelemetry).incrementCount(cImageSuccess);
    }

    @Test
    public void loadImageCallback_InvokeOnErrorCase() {
        String errorMessage = "Network problem";
        GlideException glideException = new GlideException(errorMessage, new IOException("No connectivity to server"));
        GlideImageLoader.BitmapTarget target = mImageLoader.createTarget(mRequestManager, mImageView, mCallback, URL, 100, 100);
        target.onLoadFailed(glideException, URL, target, NEEDS_SCALING);

        // Verify error callback with statuscode is called.
        verify(mCallback).onError(glideException, 0, URL);
        verify(mTelemetry).incrementCount(cImageFail);
    }

    @Test
    public void loadImageCallback_InvokeOnErrorCase_withStatusCode() {
        String errorMessage = "Network problem";
        int errorCode = 401;
        GlideException glideException = new GlideException(errorMessage, new HttpException("Unauthorized", errorCode));
        GlideImageLoader.BitmapTarget target = mImageLoader.createTarget(mRequestManager, mImageView, mCallback, URL, 100, 100);
        target.onLoadFailed(glideException, URL, target, NEEDS_SCALING);

        verify(mCallback).onError(glideException, errorCode, URL);
        verify(mTelemetry).incrementCount(cImageFail);
    }

    @Test
    public void loadImageCallback_InvokeOnErrorCase_fileNotFoundException() {
        String errorMessage = "File not found";
        int errorCode = 404;
        GlideException glideException = new GlideException(errorMessage, new FileNotFoundException(errorMessage));
        GlideImageLoader.BitmapTarget target = mImageLoader.createTarget(mRequestManager, mImageView, mCallback, URL, 100, 100);
        target.onLoadFailed(glideException, URL, target, NEEDS_SCALING);

        verify(mCallback).onError(glideException, errorCode, URL);
        verify(mTelemetry).incrementCount(cImageFail);
    }

    @Test
    public void loadImageCallback_InvokeOnErrorCase_networkTimeout() {
        String errorMessage = "Network timeout";
        int errorCode = 408;
        GlideException glideException = new GlideException(errorMessage, new SocketTimeoutException(errorMessage));
        GlideImageLoader.BitmapTarget target = mImageLoader.createTarget(mRequestManager, mImageView, mCallback, URL, 100, 100);
        target.onLoadFailed(glideException, URL, target, NEEDS_SCALING);

        verify(mCallback).onError(glideException, errorCode, URL);
        verify(mTelemetry).incrementCount(cImageFail);
    }

    @Test
    public void loadImageCallback_InvokeOnErrorCase_nullException() {
        int errorCode = 0;
        GlideException glideException = null;
        GlideImageLoader.BitmapTarget target = mImageLoader.createTarget(mRequestManager, mImageView, mCallback, URL, 100, 100);
        target.onLoadFailed(glideException, URL, target, NEEDS_SCALING);

        verify(mCallback).onError(null, errorCode, URL);
        verify(mTelemetry).incrementCount(cImageFail);
    }

    @Test
    public void loadImage_doesRequestGlideUrl_andAddsHeadersToSignature() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);
        Map<String, String> headers = Collections.singletonMap("key", "value");

        // When
        mImageLoader.loadImageInternal(
                IImageLoader.LoadImageParams.builder()
                .path(URL)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(NEEDS_SCALING)
                .headers(headers)
                .allowUpscaling(false)
                .build(),
            requestManager);

        // Then
        verify(spyBuilder).load(any(GlideUrl.class));
        ArgumentCaptor<RequestOptions> requestOptionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(requestManager).setDefaultRequestOptions(requestOptionsCaptor.capture());
        assertEquals(new ObjectKey(headers), requestOptionsCaptor.getValue().getSignature());
    }

    @Test
    public void loadImage_doesRequestString_withContentScheme() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);
        String urlWithoutScheme = "content://via.placeholder.com/300";

        // When
        mImageLoader.loadImageInternal(
            IImageLoader.LoadImageParams.builder()
                .path(urlWithoutScheme)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(NEEDS_SCALING)
                .headers(Collections.emptyMap())
                .allowUpscaling(false)
                .build(),
            requestManager);

        // Then
        verify(spyBuilder).load(any(String.class));
    }

    @Test(expected = Test.None.class)
    public void loadImage_handlesRejectedExecutionException() {
        // Given
        RejectedExecutionException ex = new RejectedExecutionException();
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(spyBuilder.listener(any())).thenThrow(ex);
        when(requestManager.asBitmap()).thenReturn(spyBuilder);

        // When
        mImageLoader.loadImageInternal(
            IImageLoader.LoadImageParams.builder()
                .path(URL)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(NEEDS_SCALING)
                .headers(Collections.emptyMap())
                .allowUpscaling(false)
                .build(),
            requestManager);

        // Verify error callback is called.
        verify(mCallback).onError(ex, URL);
        verify(mTelemetry).incrementCount(cImageFail);

    }

    @Test
    public void loadImage_doesRequestString_withContentSchemeAndHeaders() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);

        String urlWithoutScheme = "content://via.placeholder.com/300";
        Map<String, String> headers = Collections.singletonMap("key", "value");

        // When
        mImageLoader.loadImageInternal(
            IImageLoader.LoadImageParams.builder()
                .path(urlWithoutScheme)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(NEEDS_SCALING)
                .headers(headers)
                .allowUpscaling(false)
                .build(),
            requestManager
        );

        // Then
        verify(spyBuilder).load(any(String.class));
        ArgumentCaptor<RequestOptions> requestOptionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(requestManager).setDefaultRequestOptions(requestOptionsCaptor.capture());
        assertEquals(new ObjectKey(headers), requestOptionsCaptor.getValue().getSignature());
    }

    @Test
    public void loadImage_noUpscaling_setDownSampleStrategy() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);

        // When
        mImageLoader.loadImageInternal(IImageLoader.LoadImageParams.builder()
                .path(URL)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(NEEDS_SCALING)
                .allowUpscaling(false)
                .headers(Collections.emptyMap())
                .build(), requestManager);

        // Then
        ArgumentCaptor<RequestOptions> requestOptionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(requestManager).setDefaultRequestOptions(requestOptionsCaptor.capture());
        Options options = requestOptionsCaptor.getValue().getOptions();
        DownsampleStrategy strategy = options.get(Downsampler.DOWNSAMPLE_STRATEGY);
        assertTrue(strategy instanceof GlideImageLoader.NoUpscalingDownsampleStrategy);
    }

    @Test
    public void loadImage_allowUpscaling_usesDefault() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);

        // When
        mImageLoader.loadImageInternal(
            IImageLoader.LoadImageParams.builder()
                .path(URL)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(NEEDS_SCALING)
                .allowUpscaling(true)
                .headers(Collections.emptyMap())
                .build(),
            requestManager);

        // Then
        ArgumentCaptor<RequestOptions> requestOptionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(requestManager).setDefaultRequestOptions(requestOptionsCaptor.capture());
        Options options = requestOptionsCaptor.getValue().getOptions();
        DownsampleStrategy strategy = options.get(Downsampler.DOWNSAMPLE_STRATEGY);
        assertEquals(DownsampleStrategy.CENTER_OUTSIDE, strategy);
    }

    @Test
    public void loadImage_needsScaling_setsTargetSizeOriginal() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);

        // When
        mImageLoader.loadImageInternal(
            IImageLoader.LoadImageParams.builder()
                .path(URL)
                .imageView(mImageView)
                .callback(mCallback)
                .needsScaling(false)
                .allowUpscaling(true)
                .headers(Collections.emptyMap())
                .build(),
            requestManager
        );


        // Then
        CountDownLatch latch = new CountDownLatch(1);
        Target<?> target = mImageLoader.getTargets().get(mImageView).get(0);
        target.getSize((int width, int height) -> {
            assertEquals(SimpleTarget.SIZE_ORIGINAL, width);
            assertEquals(SimpleTarget.SIZE_ORIGINAL, height);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void loadImage_noScalingAndNoLayoutParams_doesNotLoad() {
        // Given
        RequestManager requestManager = spy(Glide.with(ViewhostRobolectricTest.getApplication().getApplicationContext()));
        RequestBuilder<Bitmap> spyBuilder = spy(requestManager.asBitmap());
        when(requestManager.asBitmap()).thenReturn(spyBuilder);

        ImageView imageView = new ImageView(ViewhostRobolectricTest.getApplication().getApplicationContext());

        // When
        mImageLoader.loadImageInternal(
            IImageLoader.LoadImageParams.builder()
                .path(URL)
                .imageView(imageView)
                .callback(mCallback)
                .needsScaling(true)
                .allowUpscaling(false)
                .headers(Collections.emptyMap())
                .build(),
            requestManager
        );

        // Then
        assertEquals(0, mImageLoader.getTargets().size());
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
