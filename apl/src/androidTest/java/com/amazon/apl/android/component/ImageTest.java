/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.widget.ImageView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.listeners.OnImageAttachStateChangeListener;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.views.APLImageView;
import com.amazon.apl.enums.BlendMode;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GradientUnits;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;
import com.amazon.apl.enums.NoiseFilterKind;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ImageTest extends AbstractComponentUnitTest<APLImageView, Image> {

    private static final String DUMMY_URI = "https://dummy.com/image1.png";
    private static final String DUMMY_URI1 = "custom-scheme://dummy.com/image.png";
    private static final String DUMMY_URI2 = "https://dummy.com/image2.png";

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Image Component.
        OPTIONAL_PROPERTIES =
                " \"source\": [\""+ DUMMY_URI + "\", \"" + DUMMY_URI2 + "\"]," +
                        " \"opacity\": \".75\"," +
                        " \"scale\": \"best-fit-down\"," +
                        " \"align\": \"bottom-right\"," +
                        " \"overlayColor\": \"red\"," +
                        " \"overlayGradient\": {" +
                        "     \"type\": \"linear\"," +
                        "     \"colorRange\": [\"black\", \"transparent\"]," +
                        "     \"inputRange\": [0.25, 0.75]," +
                        "     \"angle\": 45 " +
                        " }," +
                        " \"borderRadius\": \"100dp\"," +
                        " \"filters\": [" +
                        "   {" +
                        "     \"type\": \"Blur\"," +
                        "     \"radius\": \"25dp\"," +
                        "     \"source\": 0" +
                        "   }," +
                        "   {" +
                        "     \"type\": \"Noise\"," +
                        "     \"kind\": \"uniform\"," +
                        "     \"useColor\": true," +
                        "     \"sigma\": 10" +
                        "   }," +
                        "   {" +
                        "     \"type\": \"Saturate\"," +
                        "     \"amount\": 1.5" +
                        "   }," +
                        "   {" +
                        "     \"type\": \"Grayscale\"," +
                        "     \"amount\": 0.8" +
                        "   }," +
                        "   {\n" +
                        "     \"type\": \"Gradient\",\n" +
                        "     \"gradient\": {\n" +
                        "       \"type\": \"radial\",\n" +
                        "       \"colorRange\": [ \"blue\", \"red\", \"yellow\", \"purple\", \"black\"],\n" +
                        "       \"inputRange\": [ 0, 0.25, 0.55, 0.8, 1.0 ]\n" +
                        "     }" +
                        "   },\n" +
                        "   {\n" +
                        "     \"type\": \"Blend\",\n" +
                        "     \"mode\": \"color-burn\",\n" +
                        "     \"source\": -2,\n" +
                        "     \"destination\": -1\n" +
                        "   }\n" +
                        "]";
    }

    @Override
    String getComponentType() {
        return "Image";
    }

    /**
     * Test the required properties of the Component.
     * @param component The Component for testing.
     */
    @Override
    void testProperties_required(Image component) {
        assertEquals(ComponentType.kComponentTypeImage, component.getComponentType());
    }


    @Test
    @SmallTest
    @Override
    public void testComponent_shadowDefaultProperties() {
        // TODO update this to reflect processing done to bitmap
        String size = "\"width\": \"30dp\", \"height\": \"50dp\"";
        inflateDocument(buildDocument(REQUIRED_PROPERTIES), size);
        Component component = getTestComponent();

        assertEquals(0, component.getShadowOffsetHorizontal());
        assertEquals(0, component.getShadowOffsetVertical());
        assertEquals(0, component.getShadowRadius());
        assertEquals(Color.TRANSPARENT, component.getShadowColor());
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f}, component.getShadowCornerRadius(), 0.01f);
        RectF shadowBounds = component.getShadowRect();
        assertEquals(new RectF(0, 0, 0, 0), shadowBounds);
    }

    /**
     * Test the optional properties of the Component.  This test should check for default value
     * and values. No need to set {@link #OPTIONAL_PROPERTIES}.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalDefaultValues(Image component) {
        assertEquals(Arrays.asList(""), component.getSources());
        assertEquals(ImageAlign.kImageAlignCenter, component.getAlign());
        assertEquals(ImageScale.kImageScaleBestFit, component.getScale());
        assertNull(component.getOverlayGradient());
        assertEquals(0, component.getOverlayColor());
        assertEquals(0f, component.getBorderRadius().value(), 0);
        assertEquals(1f, component.getOpacity(), 0);
        assertEquals(0, component.getFilters().size());
    }

    /**
     * Test the optional properties of the Component.  This test should check values when the property
     * is set explicitly, and should use values other than the default.  Set the {@link #OPTIONAL_PROPERTIES}
     * value before this test.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalExplicitValues(Image component) {
        OPTIONAL_PROPERTIES = String.format(OPTIONAL_PROPERTIES, "[" + DUMMY_URI + ", " + DUMMY_URI2 + "]");
        assertEquals(Arrays.asList(DUMMY_URI, DUMMY_URI2), component.getSources());
        assertEquals(ImageAlign.kImageAlignBottomRight, component.getAlign());
        assertEquals(ImageScale.kImageScaleBestFitDown, component.getScale());
        assertEquals(GradientType.LINEAR, component.getOverlayGradient().getType());
        assertEquals(45, component.getOverlayGradient().getAngle(), 0);
        assertArrayEquals(new int[] { Color.BLACK, Color.TRANSPARENT }, component.getOverlayGradient().getColorRange());
        assertArrayEquals(new float[] { 0.25f, 0.75f }, component.getOverlayGradient().getInputRange(), 0);
        assertEquals(Color.RED, component.getOverlayColor());
        assertEquals(100, component.getBorderRadius().value(), 0);
        assertEquals(0.75f, component.getOpacity(), 0);

        List<Filters.Filter> expectedFilters = new ArrayList<>();
        expectedFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeBlur)
                .source(0)
                .radius(25)
                .build());
        expectedFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeNoise)
                .source(-1)
                .noiseKind(NoiseFilterKind.kFilterNoiseKindUniform)
                .noiseUseColor(true)
                .noiseSigma(10)
                .build());
        expectedFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeSaturate)
                .source(-1)
                .amount(1.5f)
                .build());
        expectedFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeGrayscale)
                .source(-1)
                .amount(0.8f)
                .build());
        expectedFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeGradient)
                .gradient(Gradient.builder()
                        .type(GradientType.RADIAL)
                        .colorRange(new int[] {
                            Color.parseColor("blue"),
                            Color.parseColor("red"),
                            Color.parseColor("yellow"),
                            Color.parseColor("purple"),
                            Color.parseColor("black"),
                        })
                        .inputRange(new float[]{ 0f, 0.25f, 0.55f, 0.8f, 1.0f })
                        .angle(Float.NaN)
                        .build())
                .build());
        expectedFilters.add(Filters.Filter.builder()
                .filterType(FilterType.kFilterTypeBlend)
                .blendMode(BlendMode.kBlendModeColorBurn)
                .source(-2)
                .destination(-1)
                .build());

        Filters actualFilters = component.getFilters();
        assertEquals(expectedFilters.size(), actualFilters.size());
        for (int i = 0; i < actualFilters.size(); i++) {
            assertEquals(expectedFilters.get(i), actualFilters.at(i));
        }

        Gradient expectedGradient = Gradient.builder()
                .type(GradientType.LINEAR)
                .colorRange(new int[] {Color.BLACK, Color.TRANSPARENT})
                .inputRange(new float[] {0.25f, 0.75f})
                .x1(1.110223E-16f)
                .y1(0)
                .x2(1)
                .y2(1)
                .spreadMethod(GradientSpreadMethod.PAD)
                .units(GradientUnits.kGradientUnitsBoundingBox)
                .angle(45f)
                .build();
        assertEquals(expectedGradient, component.getOverlayGradient());

        assertEquals(Color.RED, component.getOverlayColor());

        // verify shadow corners are same as image corner
        final float radius = component.getBorderRadius().value();
        float[] shadowRadii = component.getShadowCornerRadius();
        assertEquals(radius, shadowRadii[0], 0.01f);
        assertEquals(radius, shadowRadii[1], 0.01f);
        assertEquals(radius, shadowRadii[2], 0.01f);
        assertEquals(radius, shadowRadii[3], 0.01f);
    }


    @Test
    public void testDependencies_invokeImageLoader() {
        IImageLoaderProvider provider = mock(IImageLoaderProvider.class);
        IImageLoader imageLoader = mock(IImageLoader.class);
        when(imageLoader.withTelemetry(any(ITelemetryProvider.class))).thenReturn(imageLoader);
        when(provider.get(any(Context.class))).thenReturn(imageLoader);
        
        APLOptions options = APLOptions.builder()
                .imageProvider(provider)
                .build();

        String doc = buildDocument("\"source\": \"" + DUMMY_URI + "\"");
        inflateDocument(doc, null, options);

        Image component = getTestComponent();
        APLImageView view = (APLImageView) ComponentViewAdapterFactory.getAdapter(component).createView(mContext, mAPLPresenter);
        when(mAPLPresenter.findComponent(view)).thenReturn(component);
        ImageViewAdapter imageViewAdapter = (ImageViewAdapter)ComponentViewAdapterFactory.getAdapter(component);
        imageViewAdapter.applyAllProperties(component, view);
        OnImageAttachStateChangeListener listener = view.getOnImageAttachStateChangeListener();
        listener.onViewAttachedToWindow(view);

        verify(imageLoader).loadImage(eq(DUMMY_URI), any(ImageView.class), any(IImageLoader.LoadImageCallback2.class), any(boolean.class));
    }

    @Test
    public void testDependency_invalidUriScheme() {
        IImageLoaderProvider provider = mock(IImageLoaderProvider.class);
        IImageLoader imageLoader = mock(IImageLoader.class);
        when(imageLoader.withTelemetry(any(ITelemetryProvider.class))).thenReturn(imageLoader);
        when(provider.get(any(Context.class))).thenReturn(imageLoader);

        APLOptions options = APLOptions.builder()
                .imageProvider(provider)
                .build();

        String doc = buildDocument("\"source\": \"" + DUMMY_URI1 + "\"");
        inflateDocument(doc, null, options);

        Image component = getTestComponent();
        APLImageView view = (APLImageView) ComponentViewAdapterFactory.getAdapter(component).createView(mContext, mAPLPresenter);
        when(mAPLPresenter.findComponent(view)).thenReturn(component);
        ImageViewAdapter imageViewAdapter = (ImageViewAdapter)ComponentViewAdapterFactory.getAdapter(component);
        imageViewAdapter.applyAllProperties(component, view);

        verify(imageLoader, never()).loadImage(eq(DUMMY_URI), any(ImageView.class), any(IImageLoader.LoadImageCallback2.class), any(boolean.class));
        verify(imageLoader, never()).loadImage(eq(DUMMY_URI1), any(ImageView.class), any(IImageLoader.LoadImageCallback2.class), any(boolean.class));
    }

    @Test
    public void testDependency_customUriScheme() {
        IImageLoaderProvider provider = mock(IImageLoaderProvider.class);
        IImageLoader imageLoader = mock(IImageLoader.class);
        when(imageLoader.withTelemetry(any(ITelemetryProvider.class))).thenReturn(imageLoader);
        when(provider.get(any(Context.class))).thenReturn(imageLoader);
        Set<String> uriSchemes = new HashSet<>();
        uriSchemes.add("custom-scheme");

        APLOptions options = APLOptions.builder()
                .imageProvider(provider)
                .imageUriSchemeValidator((scheme, aplVersion) -> "custom-scheme".equals(scheme))
                .build();

        String doc = buildDocument("\"source\": \"" + DUMMY_URI1 + "\"");
        inflateDocument(doc, null, options);

        Image component = getTestComponent();
        APLImageView view = (APLImageView) ComponentViewAdapterFactory.getAdapter(component).createView(mContext, mAPLPresenter);
        when(mAPLPresenter.findComponent(view)).thenReturn(component);
        ImageViewAdapter imageViewAdapter = (ImageViewAdapter)ComponentViewAdapterFactory.getAdapter(component);
        imageViewAdapter.applyAllProperties(component, view);
        OnImageAttachStateChangeListener listener = view.getOnImageAttachStateChangeListener();
        listener.onViewAttachedToWindow(view);

        verify(imageLoader).loadImage(eq(DUMMY_URI1), any(ImageView.class), any(IImageLoader.LoadImageCallback2.class), any(boolean.class));
    }

    private Bitmap createDummyBitmap() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
