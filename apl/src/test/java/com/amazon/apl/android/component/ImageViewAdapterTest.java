/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.image.ImageBitmapKey;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ImageViewAdapterTest extends AbstractComponentViewAdapterTest<Image, APLImageView> {
    @Mock
    private Image mImage;
    @Mock
    private Filters mockFilters;
    @Mock
    private IImageLoaderProvider mockImageLoaderProvider;
    @Mock
    private IImageLoader mockImageLoader;
    @Mock
    private IImageUriSchemeValidator mockImageUriSchemeValidator;
    @Mock
    private IBitmapCache mockBitmapCache;
    @Mock
    private ITelemetryProvider mockTelemetryProvider;

    private static final int METRIC_CACHE_MISS = 0;
    private static final int METRIC_CACHE_HIT = 1;

    @Override
    Image component() {
        return mImage;
    }

    @Override
    void componentSetup() {
        when(mRenderingContext.getBitmapCache()).thenReturn(mockBitmapCache);
        when(mRenderingContext.getTelemetryProvider()).thenReturn(mockTelemetryProvider);
        when(component().getSources()).thenReturn(Arrays.asList("https://dummy.image1.com"));
        when(mockFilters.size()).thenReturn(0);
        when(mockImageLoader.withTelemetry(any(ITelemetryProvider.class))).thenReturn(mockImageLoader);
        when(mockImageLoaderProvider.get(any(Context.class))).thenReturn(mockImageLoader);
        when(mockImageUriSchemeValidator.isUriSchemeValid(anyString(), anyInt())).thenReturn(true);
        when(component().getFilters()).thenReturn(mockFilters);
        when(component().getImageLoader(any(Context.class))).thenReturn(mockImageLoader);
        when(component().getImageLoader(null)).thenReturn(mockImageLoader);
        when(component().getImageLoaderProvider()).thenReturn(mockImageLoaderProvider);
        when(component().getUriSchemeValidator()).thenReturn(mockImageUriSchemeValidator);
        when(component().getScale()).thenReturn(ImageScale.kImageScaleFill);
        when(component().getAlign()).thenReturn(ImageAlign.kImageAlignCenter);

        when(mockTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, ImageViewAdapter.METRIC_COUNTER_BITMAP_CACHE_MISS, ITelemetryProvider.Type.COUNTER)).thenReturn(METRIC_CACHE_MISS);
        when(mockTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, ImageViewAdapter.METRIC_COUNTER_BITMAP_CACHE_HIT, ITelemetryProvider.Type.COUNTER)).thenReturn(METRIC_CACHE_HIT);
    }

    @Test
    public void test_imageLoader_invoked_for_single_source() {
        applyAllProperties();
        getView().getOnImageAttachStateChangeListener().onViewAttachedToWindow(getView());
        verify(mockImageLoader).loadImage(eq("https://dummy.image1.com"), any(APLImageView.class), any(IImageLoader.LoadImageCallback2.class), eq(true));
        verify(mockTelemetryProvider).incrementCount(METRIC_CACHE_MISS);
        verify(mockTelemetryProvider, never()).incrementCount(METRIC_CACHE_HIT);
    }

    @Test
    public void test_imageLoader_invoked_for_multiple_sources() {
        ArgumentCaptor<IImageLoader.LoadImageCallback2> callbackCaptor = ArgumentCaptor.forClass(IImageLoader.LoadImageCallback2.class);
        when(component().getSources()).thenReturn(Arrays.asList("https://dummy.image1.com", "https://dummy.image2.com"));
        when(mMockPresenter.findComponent(getView())).thenReturn(component());
        applyAllProperties();
        getView().getOnImageAttachStateChangeListener().onViewAttachedToWindow(getView());
        verify(mockImageLoader).loadImage(eq("https://dummy.image1.com"), any(APLImageView.class), callbackCaptor.capture(), eq(true));
        callbackCaptor.getValue().onSuccess(createDummyBitmap(), "https://dummy.image1.com");
        verify(mockImageLoader).loadImage(eq("https://dummy.image2.com"), any(APLImageView.class), callbackCaptor.capture(), eq(true));
    }

    @Test
    public void test_cachedBitmap_returned() {
        ImageBitmapKey key = ImageBitmapKey.create(mImage);
        Bitmap dummyBitmap = createDummyBitmap();
        when(mockBitmapCache.getBitmap(key)).thenReturn(dummyBitmap);

        when(mMockPresenter.findComponent(getView())).thenReturn(component());
        applyAllProperties();
        verify(mockBitmapCache).getBitmap(key);
        verify(mockTelemetryProvider).incrementCount(METRIC_CACHE_HIT);
        verify(mockTelemetryProvider, never()).incrementCount(METRIC_CACHE_MISS);
        assertNull(getView().getOnImageAttachStateChangeListener());
        verifyZeroInteractions(mockImageLoader);
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
