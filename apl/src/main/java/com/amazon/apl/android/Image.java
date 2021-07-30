/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.BitmapKey;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.image.ImageShadowBoundsCalculator;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;
import com.amazon.apl.enums.PropertyKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a APL Image component.
 */
public class Image extends Component implements BitmapKey {
    private final IImageLoaderProvider mImageLoaderProvider;
    private final IImageProcessor mImageProcessor;
    private final IImageUriSchemeValidator mUriSchemeValidator;
    private final IExtensionImageFilterCallback mExtensionImageFilterCallback;
    private final IBitmapFactory mBitmapFactory;
    private final int mAplVersion;
    private boolean mShouldDrawShadow;
    private RectF mShadowBounds = new RectF();

    /**
     * Image constructor.
     * {@inheritDoc}
     */
    Image(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
        mImageLoaderProvider = renderingContext.getImageLoaderProvider();
        mImageProcessor = renderingContext.getImageProcessor();
        mUriSchemeValidator = renderingContext.getImageUriSchemeValidator();
        mAplVersion = renderingContext.getDocVersion();
        mBitmapFactory = renderingContext.getBitmapFactory();
        mExtensionImageFilterCallback = renderingContext.getExtensionImageFilterCallback();
    }

    public boolean hasShadow() {
        return getShadowOffsetHorizontal() != 0
                || getShadowOffsetVertical() != 0
                || getShadowRadius() > 0;
    }

    @Override
    public boolean shouldDrawBoxShadow() {
        return mShouldDrawShadow;
    }

    public void setShouldDrawBoxShadow(boolean shouldDrawBoxShadow) {
        mShouldDrawShadow = shouldDrawBoxShadow;
    }

    @Override
    public RectF getShadowRect() {
        return mShadowBounds;
    }

    public void setShadowBounds(Bitmap image, ImageAlign align) {
        mShadowBounds = ImageShadowBoundsCalculator.builder()
                .bounds(getBounds())
                .innerBounds(getInnerBounds())
                .align(align)
                .image(image)
                .offsetX(getShadowOffsetHorizontal())
                .offsetY(getShadowOffsetVertical())
                .build()
                .calculateShadowBounds();
    }

    public IImageUriSchemeValidator getUriSchemeValidator() {
        return mUriSchemeValidator;
    }

    public int getAplVersion() {
        return mAplVersion;
    }

    public IImageLoaderProvider getImageLoaderProvider() {
        return mImageLoaderProvider;
    }

    public IImageProcessor getImageProcessor() {
        return mImageProcessor;
    }

    public IExtensionImageFilterCallback getExtensionImageFilterCallback() {
        return mExtensionImageFilterCallback;
    }

    public IBitmapFactory getBitmapFactory() {
        return mBitmapFactory;
    }

    /**
     * @param context The Android Context.
     * @return The dependency injected image loader.
     */
    public IImageLoader getImageLoader(Context context) {
        return mImageLoaderProvider.get(context)
                .withTelemetry(getRenderingContext().getTelemetryProvider());
    }

    /**
     * Returns the list of Sources as validated by the URI scheme validator.
     *
     * Note: filters may not work properly if an image fails validation.
     *
     * @return a list of Urls to download.
     */
    public final List<String> getSources() {
        List<String> validatedSources = new ArrayList<>();
        String[] sources = mProperties.getStringArray(PropertyKey.kPropertySource);
        if (sources != null) {
            for (String source : sources) {
                if (mUriSchemeValidator.isUriSchemeValid(Uri.parse(source).getScheme(), getAplVersion())) {
                    validatedSources.add(source);
                }
            }
        }

        return validatedSources;
    }

    /**
     * @return Alignment of the image within the containing box. Defaults to center.
     */
    public final ImageAlign getAlign() {
        return ImageAlign.valueOf(mProperties.getEnum(PropertyKey.kPropertyAlign));
    }

    /**
     * @return How the image will be resized to fit in the bounding box. Defaults to best-fit.
     */
    public final ImageScale getScale() {
        return ImageScale.valueOf(mProperties.getEnum(PropertyKey.kPropertyScale));
    }

    /**
     * @return Colored gradient that overlays the image.
     */
    @Nullable
    public final Gradient getOverlayGradient() {
        return mProperties.getGradient(PropertyKey.kPropertyOverlayGradient);
    }

    /**
     * @return If set, a scrim will on the image. Defaults to transparent.
     */
    public final int getOverlayColor() {
        return mProperties.getColor(PropertyKey.kPropertyOverlayColor);
    }

    /**
     * @return Clipping radius for the image. Defaults to 0.
     */
    @Nullable
    public final Dimension getBorderRadius() {
        return mProperties.getDimension(PropertyKey.kPropertyBorderRadius);
    }

    /**
     * @return Opacity of the image.
     */
    public final float getOpacity() {
        return mProperties.getFloat(PropertyKey.kPropertyOpacity);
    }

    /**
     * @return Filtering operations to apply to the image.
     */
    @NonNull
    public final Filters getFilters() {
        return mProperties.getFilters(PropertyKey.kPropertyFilters);
    }

    /**
     * @return - Shadow corner radius as a float array
     */
    @Override
    public float[] getShadowCornerRadius() {
        final float radius = getBorderRadius().value();
        return new float[] {radius, radius, radius, radius};
    }
}