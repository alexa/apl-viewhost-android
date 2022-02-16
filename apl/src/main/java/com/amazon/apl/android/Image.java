/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageLoader;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.image.ImageShadowBoundsCalculator;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;
import com.amazon.apl.enums.PropertyKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a APL Image component.
 */
public class Image extends Component {
    private final IImageLoaderProvider mImageLoaderProvider;
    private final IImageProcessor mImageProcessor;
    private final IImageUriSchemeValidator mUriSchemeValidator;
    private final IExtensionImageFilterCallback mExtensionImageFilterCallback;
    private final IBitmapFactory mBitmapFactory;
    private final int mAplVersion;
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

    @Override
    public RectF getShadowRect() {
        return mShadowBounds;
    }

    public void setShadowBounds(Rect clipPath) {
        if (shouldDrawBoxShadow()) {
            mShadowBounds = ImageShadowBoundsCalculator.builder()
                    .bounds(getBounds())
                    .innerBounds(getInnerBounds())
                    .align(getAlign())
                    .imageBounds(clipPath)
                    .offsetX(getShadowOffsetHorizontal())
                    .offsetY(getShadowOffsetVertical())
                    .build()
                    .calculateShadowBounds();
            // Prepare the shadow and invalidate the parent.
            ShadowBitmapRenderer shadowBitmapRenderer = getViewPresenter().getShadowRenderer();
            if (shadowBitmapRenderer != null) {
                shadowBitmapRenderer.prepareShadow(this);
                View parent = getViewPresenter().findView(getParent());
                if (parent != null) {
                    parent.invalidate();
                }
            }
        }
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
     * @return a list of {@link UrlRequests.UrlRequest} to download.
     */
    public final List<UrlRequests.UrlRequest> getSourceRequests() {
        List<UrlRequests.UrlRequest> validatedSources = new ArrayList<>();
        UrlRequests urlRequests = mProperties.getUrlRequests(PropertyKey.kPropertySource);

        if (urlRequests != null && urlRequests.size() > 0) {
            for (UrlRequests.UrlRequest request : urlRequests) {
                if (request.url() != null &&
                        !request.url().isEmpty() &&
                        mUriSchemeValidator.isUriSchemeValid(Uri.parse(request.url()).getScheme(), getAplVersion())) {
                    validatedSources.add(request);
                }
            }
        }

        return validatedSources;
    }

    /**
     * Returns the list of Sources as validated by the URI scheme validator.
     *
     * Note: filters may not work properly if an image fails validation.
     *
     * @return a list of source urls to download.
     */
    public final List<String> getSourceUrls() {
        List<String> urls = new ArrayList<>();
        for(UrlRequests.UrlRequest request : getSourceRequests()) {
            urls.add(request.url());
        }
        return urls;
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
        // gradient is not a defaulted property, check if it is set before fetch
        if (!mProperties.hasProperty(PropertyKey.kPropertyOverlayGradient))
            return null;
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