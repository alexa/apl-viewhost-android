/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.graphics.Bitmap;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.component.ImageViewAdapter;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.bumptech.glide.load.engine.GlideException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LazyImageLoaderTest extends ViewhostRobolectricTest {
    private static final Bitmap BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    @Mock
    private IAPLViewPresenter mockPresenter;
    @Mock
    private Image mImage;
    @Mock
    private ImageViewAdapter mImageViewAdapter;
    @Mock
    private IImageLoader mImageLoader;
    @Mock
    private IDTNetworkRequestHandler mDTNetworkRequest;

    private APLImageView mImageView;

    private UrlRequests.UrlRequest mSource;

    private List<UrlRequests.UrlRequest> mSources;

    @Before
    public void setup() {
        mSource = UrlRequests.UrlRequest.builder().url("https://test.com").build();
        mSources = Collections.singletonList(mSource);
        // Prepare mock items
        mImageView = new APLImageView(ViewhostRobolectricTest.getApplication().getApplicationContext(), mockPresenter);
        when(mImage.getSourceRequests()).thenReturn(mSources);
        when(mImage.getImageLoader(any())).thenReturn(mImageLoader);
    }

    @Test
    public void initImageLoading_onSuccess() {
        // Given
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onSuccess(BITMAP, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        // When
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView, mDTNetworkRequest);

        // Then
        verify(mockPresenter).mediaLoaded(mSource.url());
        verify(mDTNetworkRequest).loadingFinished(anyInt(), anyDouble(), anyInt());
    }

    @Test
    public void initImageLoading_multipleSourcesLoadsBitmaps() {
        // Given
        UrlRequests.UrlRequest source1 = UrlRequests.UrlRequest.builder()
                .url("https://test.com")
                .headers(Collections.singletonMap("key", "value"))
                .build();
        UrlRequests.UrlRequest source2 = UrlRequests.UrlRequest.builder()
                .url("https://test.com").build();

        List<UrlRequests.UrlRequest> sources = new ArrayList<>();
        sources.add(source1);
        sources.add(source2);
        when(mImage.getSourceRequests()).thenReturn(sources);
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onSuccess(BITMAP, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        // When
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView, mDTNetworkRequest);

        // Then
        ArgumentCaptor<List<Bitmap>> bitmapCaptor = ArgumentCaptor.forClass(List.class);
        verify(mImageViewAdapter).onImageLoad(any(), bitmapCaptor.capture());
        List<Bitmap> bitmaps = bitmapCaptor.getValue();
        assertEquals(2, bitmaps.size());
        assertEquals(BITMAP, bitmaps.get(0));
        assertEquals(BITMAP, bitmaps.get(1));
    }

    @Test
    public void initImageLoading_handlesMultipleCallbacks() {
        // Given
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onSuccess(BITMAP, load.path());
            load.callback().onSuccess(BITMAP, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        // When
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView, mDTNetworkRequest);

        // Then
        verify(mImageViewAdapter, times(1)).onImageLoad(any(), any());
    }

    @Test
    public void initImageLoading_onError() {
        // Given
        String message = "I failed";
        GlideException exception = new GlideException(message);
        int errorCode = 0;
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onError(exception, errorCode, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        // When
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView, mDTNetworkRequest);

        // Then
        verify(mockPresenter).mediaLoadFailed(mSource.url(), errorCode, message);
        verify(mImageViewAdapter).onImageLoad(any(), any());
        verify(mDTNetworkRequest).loadingFailed(anyInt(), anyDouble());
    }

    @Test
    public void initImageLoading_onError_nullException() {
        // Given
        int errorCode = 0;
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onError(null, errorCode, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        // When
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView, mDTNetworkRequest);

        // Then
        verify(mockPresenter).mediaLoadFailed(eq(mSource.url()), eq(errorCode), anyString());
        verify(mDTNetworkRequest).loadingFailed(anyInt(), anyDouble());
    }

    @Test
    public void initImageLoading_onFileUrl_successfulLoadingWithNoDTNetworkInteraction() {
        // Given
        UrlRequests.UrlRequest source = UrlRequests.UrlRequest.builder()
                .url("File:android_asset//test.png")
                .headers(Collections.singletonMap("key", "value"))
                .build();
        List<UrlRequests.UrlRequest> sources = new ArrayList<>();
        sources.add(source);
        when(mImage.getSourceRequests()).thenReturn(sources);

        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onSuccess(BITMAP, load.path());
            return null;
        }).when(mImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        // When
        LazyImageLoader.initImageLoad(mImageViewAdapter, mImage, mImageView, mDTNetworkRequest);

        // Then
        verify(mockPresenter).mediaLoaded(anyString());
        verifyNoInteractions(mDTNetworkRequest);
    }
}
