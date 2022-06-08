/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.amazon.apl.android.Image;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.image.ImageProcessingAsyncTask;
import com.amazon.apl.android.image.ProcessedImageBitmapKey;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;
import com.amazon.apl.enums.PropertyKey;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    @Mock
    private IBitmapFactory mockBitmapFactory;
    @Mock
    private IImageProcessor mockImageProcessor;
    @Mock
    private IExtensionImageFilterCallback mockExtensionFilterCallback;

    private static final int METRIC_CACHE_MISS = 0;
    private static final int METRIC_CACHE_HIT = 1;

    @Override
    Image component() {
        return mImage;
    }

    @Override
    void componentSetup() throws BitmapCreationException {
        when(mRenderingContext.getBitmapCache()).thenReturn(mockBitmapCache);
        when(mRenderingContext.getTelemetryProvider()).thenReturn(mockTelemetryProvider);
        UrlRequests.UrlRequest source1 = UrlRequests.UrlRequest.builder().url("https://dummy.image1.com").build();
        when(component().getSourceRequests()).thenReturn(Arrays.asList(source1));
        when(mockFilters.size()).thenReturn(0);
        when(mockImageLoader.withTelemetry(any(ITelemetryProvider.class))).thenReturn(mockImageLoader);
        when(mockImageLoaderProvider.get(any(Context.class))).thenReturn(mockImageLoader);
        when(mockImageUriSchemeValidator.isUriSchemeValid(anyString(), anyInt())).thenReturn(true);
        when(mockBitmapFactory.createBitmap(any(Bitmap.class))).thenReturn(createDummyBitmap());
        when(component().getFilters()).thenReturn(mockFilters);
        when(component().getImageLoader(any(Context.class))).thenReturn(mockImageLoader);
        when(component().getImageLoader(null)).thenReturn(mockImageLoader);
        when(component().getImageLoaderProvider()).thenReturn(mockImageLoaderProvider);
        when(component().getUriSchemeValidator()).thenReturn(mockImageUriSchemeValidator);
        when(component().getScale()).thenReturn(ImageScale.kImageScaleFill);
        when(component().getAlign()).thenReturn(ImageAlign.kImageAlignCenter);
        when(component().getBitmapFactory()).thenReturn(mockBitmapFactory);
        when(component().getImageProcessor()).thenReturn(mockImageProcessor);
        when(component().getExtensionImageFilterCallback()).thenReturn(mockExtensionFilterCallback);
        when(mMockPresenter.findComponent(getView())).thenReturn(component());

        when(mockTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, ImageViewAdapter.METRIC_COUNTER_BITMAP_CACHE_MISS, ITelemetryProvider.Type.COUNTER)).thenReturn(METRIC_CACHE_MISS);
        when(mockTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, ImageViewAdapter.METRIC_COUNTER_BITMAP_CACHE_HIT, ITelemetryProvider.Type.COUNTER)).thenReturn(METRIC_CACHE_HIT);
    }

    @Test
    public void test_imageLoader_invoked_for_single_source() {
        applyAllProperties();
        verify(mockImageLoader).loadImage(argThat(load ->
                "https://dummy.image1.com".equals(load.path())
                        && load.needsScaling()
                        && !load.allowUpscaling()
            )
        );
        verify(mockTelemetryProvider).incrementCount(METRIC_CACHE_MISS);
        verify(mockTelemetryProvider, never()).incrementCount(METRIC_CACHE_HIT);
    }

    @Test
    public void test_imageLoader_invoked_for_multiple_sources() {
        UrlRequests.UrlRequest source1 = UrlRequests.UrlRequest.builder().url("https://dummy.image1.com").build();
        UrlRequests.UrlRequest source2 = UrlRequests.UrlRequest.builder().url("https://dummy.image2.com").build();

        when(component().getSourceRequests()).thenReturn(Arrays.asList(source1, source2));
        when(mMockPresenter.findComponent(getView())).thenReturn(component());
        applyAllProperties();
        // Upscaling is allowed for blending to maintain legacy behavior
        verify(mockImageLoader).loadImage(argThat(load ->
                        "https://dummy.image1.com".equals(load.path())
                                && load.needsScaling()
                                && load.allowUpscaling()
                )
        );
        verify(mockImageLoader).loadImage(argThat(load ->
                        "https://dummy.image2.com".equals(load.path())
                                && load.needsScaling()
                                && load.allowUpscaling()
                )
        );
    }

    @Test
    public void test_cachedBitmap_returned() {
        ProcessedImageBitmapKey key = ProcessedImageBitmapKey.create(mImage);
        Bitmap dummyBitmap = createDummyBitmap();
        when(mockBitmapCache.getBitmap(key)).thenReturn(dummyBitmap);

        when(mMockPresenter.findComponent(getView())).thenReturn(component());
        applyAllProperties();
        verify(mockBitmapCache).getBitmap(key);
        verify(mockTelemetryProvider).incrementCount(METRIC_CACHE_HIT);
        verify(mockTelemetryProvider, never()).incrementCount(METRIC_CACHE_MISS);
        verifyZeroInteractions(mockImageLoader);
    }

    @Test
    public void test_layoutNotTriggeredOnImageLoad() {
        APLImageView imageView = spy(getView());
        ProcessedImageBitmapKey key = ProcessedImageBitmapKey.create(mImage);
        Bitmap dummyBitmap = createDummyBitmap();
        when(mockBitmapCache.getBitmap(key)).thenReturn(dummyBitmap);
        when(mMockPresenter.findComponent(getView())).thenReturn(component());

        applyAllProperties(imageView);
        verify(imageView).setLayoutRequestsEnabled(false);
        verify(imageView).setImageDrawable(any(Drawable.class));
        verify(imageView).setLayoutRequestsEnabled(true);
    }

    @Test
    public void test_layoutTriggeredOnImageLoadForShadow() {
        APLImageView imageView = spy(getView());
        ProcessedImageBitmapKey key = ProcessedImageBitmapKey.create(mImage);
        when(component().shouldDrawBoxShadow()).thenReturn(true);
        Bitmap dummyBitmap = createDummyBitmap();
        when(mockBitmapCache.getBitmap(key)).thenReturn(dummyBitmap);
        when(mMockPresenter.findComponent(getView())).thenReturn(component());
        when(imageView.getParent()).thenReturn((ViewParent) mock(ViewGroup.class));

        applyAllProperties(imageView);
        verify(imageView, times(2)).setLayoutRequestsEnabled(true);
        verify(imageView).setImageDrawable(any(Drawable.class));
    }

    @Test
    public void testOnImageLoad() {
        ImageViewAdapter imageViewAdapter = (ImageViewAdapter) getAdapter();
        APLImageView spyView = spy(getView());
        when(mMockPresenter.findComponent(spyView)).thenReturn(component());
        applyAllProperties(spyView);
        List<Bitmap> bitmapList = new ArrayList<>();
        bitmapList.add(createDummyBitmap());
        imageViewAdapter.onImageLoad(spyView, bitmapList);
        verify(spyView).setImageProcessingAsyncTask(any(ImageProcessingAsyncTask.class));
    }

    @Test
    public void test_onImageLoadWithMultipleSources_usesLastBitmap() {
        UrlRequests.UrlRequest source1 = UrlRequests.UrlRequest.builder().url("https://dummy.image1.com").build();
        UrlRequests.UrlRequest source2 = UrlRequests.UrlRequest.builder().url("https://dummy.image2.com").build();

        when(component().getSourceRequests()).thenReturn(Arrays.asList(source1, source2));
        when(mMockPresenter.findComponent(getView())).thenReturn(component());
        when(component().getImageProcessor()).thenReturn(null);

        Bitmap first = createDummyBitmap();
        Bitmap second = createDummyBitmap();

        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            Bitmap result = load.path().equals(source1.url()) ? first : second;
            load.callback().onSuccess(result, load.path());
            return null;
        }).when(mockImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        applyAllProperties();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) getView().getDrawable();
        assertEquals(second, bitmapDrawable.getBitmap());
    }

    @Test
    public void testOnImageLoad_noProcessing_usesOriginalBitmap() {
        when(component().getImageProcessor()).thenReturn(null);
        Bitmap expected = createDummyBitmap();
        doAnswer(invocation -> {
            IImageLoader.LoadImageParams load = invocation.getArgument(0);
            load.callback().onSuccess(expected, load.path());
            return null;
        }).when(mockImageLoader).loadImage(any(IImageLoader.LoadImageParams.class));

        applyAllProperties();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) getView().getDrawable();
        assertEquals(expected, bitmapDrawable.getBitmap());
    }

    @Test
    public void test_refreshProperties_borderRadius() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)));

        Path mockPath = mock(Path.class);
        getView().setClipPath(mockPath);
        when(component().getBorderRadius()).thenReturn(Dimension.create(10));

        refreshProperties(PropertyKey.kPropertyBorderRadius);

        assertEquals(10f, getView().getBorderRadius(), 0.01f);
        verify(mockPath).addRoundRect(eq(0f), eq(0f), eq(200f), eq(200f), eq(10f), eq(10f), eq(Path.Direction.CCW));
    }

    @Test
    public void test_refreshProperties_align_left() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));
        when(component().getAlign()).thenReturn(ImageAlign.kImageAlignLeft);

        refreshProperties(PropertyKey.kPropertyAlign);

        assertEquals(ImageAlign.kImageAlignLeft, getView().getImageAlign());

        Matrix matrix = getView().getImageMatrix();
        Matrix expected = new Matrix();
        expected.setScale(2, 2); // 0,0 - 50x100 -> 0,0 - 100x200
        expected.postTranslate(0, 0); // 0,0 - 100x200 -> 0,0 - 100x200
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_align_right() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));
        when(component().getAlign()).thenReturn(ImageAlign.kImageAlignRight);

        refreshProperties(PropertyKey.kPropertyAlign);

        assertEquals(ImageAlign.kImageAlignRight, getView().getImageAlign());

        Matrix matrix = getView().getImageMatrix();
        Matrix expected = new Matrix();
        expected.setScale(2, 2); // 0,0 - 50x100 -> 0,0 - 100x200
        expected.postTranslate(100, 0); // 0,0 - 100x200 -> 100,0 - 100x200
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_align_bottom() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)));
        when(component().getAlign()).thenReturn(ImageAlign.kImageAlignBottom);

        refreshProperties(PropertyKey.kPropertyAlign);

        assertEquals(ImageAlign.kImageAlignBottom, getView().getImageAlign());

        Matrix matrix = getView().getImageMatrix();
        Matrix expected = new Matrix();
        expected.setScale(2, 2); // 0,0 - 100x50 -> 0,0 - 200x100
        expected.postTranslate(0, 100); // 0,0 - 200x100 -> 0,100 - 200x100
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_scale_bestFill() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));

        when(component().getScale()).thenReturn(ImageScale.kImageScaleBestFill);

        refreshProperties(PropertyKey.kPropertyScale);

        assertEquals(ImageScale.kImageScaleBestFill, getView().getImageScale());

        Matrix matrix = getView().getImageMatrix();
        Matrix expected = new Matrix();
        expected.setScale(4, 4); // 0,0 - 50x100 -> 0,0 - 200x400
        expected.postTranslate(0, -100); // 0,0 - 200x400 -> 0,-100 - 200x400
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_scale_fill() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));

        when(component().getScale()).thenReturn(ImageScale.kImageScaleFill);

        refreshProperties(PropertyKey.kPropertyScale);

        assertEquals(ImageScale.kImageScaleFill, getView().getImageScale());

        Matrix matrix = getView().getImageMatrix();
        Matrix expected = new Matrix();
        expected.setScale(4, 2); // 0,0 - 50x100 -> 0,0 - 200x200
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_scale_bestFitDown() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));

        when(component().getScale()).thenReturn(ImageScale.kImageScaleBestFitDown);

        refreshProperties(PropertyKey.kPropertyScale);

        assertEquals(ImageScale.kImageScaleBestFitDown, getView().getImageScale());

        Matrix matrix = getView().getImageMatrix();
        Matrix expected = new Matrix();
        expected.postTranslate(75, 50); // 0,0 - 50x100 -> 75,50 - 50x100
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_scale_none() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));

        when(component().getScale()).thenReturn(ImageScale.kImageScaleNone);

        refreshProperties(PropertyKey.kPropertyScale);

        // changing to none triggers a new request from ImageLoader with needsScaling false.
        verify(mockImageLoader).loadImage(argThat(load ->
                        !load.needsScaling()
                )
        );
        assertEquals(ImageScale.kImageScaleNone, getView().getImageScale());

        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));
        Matrix matrix = getView().getImageMatrix();

        Matrix expected = new Matrix();
        expected.postTranslate(75, 50); // 0,0 - 50x100 -> 75,50 - 50x100
        assertEquals(expected, matrix);
    }

    @Test
    public void test_refreshProperties_overlayColor() {
        when(component().getOverlayColor()).thenReturn(Color.argb(50, 255, 0, 0));

        refreshProperties(PropertyKey.kPropertyOverlayColor);

        Paint overlayPaint = getView().getOverlayPaint();
        assertEquals(50, overlayPaint.getAlpha());
        assertEquals(Color.argb(50, 255, 0, 0), overlayPaint.getColor());
    }

    @Test
    public void test_refreshProperties_overlayGradient() {
        getView().layout(0, 0, 200, 200);
        getView().setImageDrawable(new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)));

        Gradient gradient = Gradient.builder()
                .type(GradientType.LINEAR)
                .inputRange(new float[] {0.25f, 0.75f})
                .colorRange(new int[]{Color.BLACK, Color.TRANSPARENT})
                .angle(90f)
                .build();

        when(component().getOverlayGradient()).thenReturn(gradient);

        refreshProperties(PropertyKey.kPropertyOverlayGradient);

        assertEquals(gradient, getView().getOverlayGradient());
        Paint overlayGradient = getView().getOverlayGradientPaint();
        // Can't compare Shaders directly. Need to use their matrices instead.
        Shader actual = overlayGradient.getShader();
        Matrix actualMatrix = new Matrix();
        actual.getLocalMatrix(actualMatrix);

        // Scaled bitmap is 100x200
        Shader expected = gradient.getShader(100, 200);
        Matrix expectedMatrix = new Matrix();
        expected.getLocalMatrix(expectedMatrix);
        assertEquals(expectedMatrix, actualMatrix);
    }

    @Test
    public void test_draw_onCanvas() {
        Canvas mockCanvas = mock(Canvas.class);
        getView().layout(0, 0, 220, 220);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getApplication().getResources(), Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
        getView().setImageDrawable(bitmapDrawable);
        getView().setPadding(10, 10, 10, 10);
        getView().setOverlayColor(Color.RED);
        getView().setOverlayGradient(Gradient.builder()
                .type(GradientType.LINEAR)
                .inputRange(new float[] {0.25f, 0.75f})
                .colorRange(new int[]{Color.BLACK, Color.TRANSPARENT})
                .angle(90f)
                .build());

        getView().drawToCanvas(mockCanvas);

        InOrder inOrder = inOrder(mockCanvas);
        // save current
        inOrder.verify(mockCanvas).save();
        // move to padding
        inOrder.verify(mockCanvas).translate(10, 10);
        // clip the thing
        inOrder.verify(mockCanvas).clipPath(getView().getClipPath());
        // save to perform image scaling matrix
        inOrder.verify(mockCanvas).save();
        // scale with the image matrix
        inOrder.verify(mockCanvas).concat(getView().getImageMatrix());
        // draw the bitmap call can't be verified unless drawable is mocked.

        // restore the image matrix
        inOrder.verify(mockCanvas).restore();
        // draw the overlay color
        inOrder.verify(mockCanvas).drawRect(eq(0f), eq(0f), eq(220f), eq(220f), eq(getView().getOverlayPaint()));
        // draw the overlay gradient
        inOrder.verify(mockCanvas).drawRect(eq(0f), eq(0f), eq(200f), eq(200f), eq(getView().getOverlayGradientPaint()));
        // final restore call
        inOrder.verify(mockCanvas).restoreToCount(anyInt());
    }

    @Test
    public void testZeroSizeImage_applyProperties_doesNotLoadSource() {
        Rect zeroSize = Rect.builder().left(0).top(0).width(0).height(0).build();
        when(component().getInnerBounds()).thenReturn(zeroSize);

        applyAllProperties();

        verifyZeroInteractions(mockImageLoader);
    }

    @Test
    public void testZeroSizeImage_resized_refreshProperties_loadsSource() {
        Rect zeroSize = Rect.builder().left(0).top(0).width(0).height(0).build();
        when(component().getInnerBounds()).thenReturn(zeroSize);

        applyAllProperties();

        when(component().getInnerBounds()).thenReturn(Rect.builder().left(0).top(0).width(10).height(10).build());

        refreshProperties(PropertyKey.kPropertyBounds);

        verify(mockImageLoader).loadImage(any());
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
