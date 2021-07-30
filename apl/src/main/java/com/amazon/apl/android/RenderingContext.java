/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.net.Uri;

import com.amazon.apl.android.bitmap.BitmapFactory;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.bitmap.NoOpBitmapCache;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpImageLoaderProvider;
import com.amazon.apl.android.providers.impl.NoOpMediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scaling.NoOpMetricsTransform;

/**
 * Contains context needed to render a document. This class serves as a convenient way to pass
 * context from the top of the application (RootContext) down to all levels of the code base.
 */
public class RenderingContext {

    private final int docVersion;
    private IMetricsTransform metricsTransform;
    private final ITextMeasurementCache textMeasurementCache;
    private final ITelemetryProvider telemetryProvider;
    private final AbstractMediaPlayerProvider mediaPlayerProvider;
    private final IImageLoaderProvider imageLoaderProvider;
    private final IImageProcessor imageProcessor;
    private final IImageUriSchemeValidator imageUriSchemeValidator;
    private final IExtensionImageFilterCallback extensionImageFilterCallback;
    private final IBitmapFactory bitmapFactory;
    private final IBitmapCache bitmapCache;
    private final IContentRetriever<Uri, String> avgRetriever;

    private RenderingContext(
            int docVersion,
            IMetricsTransform metricsTransform,
            ITextMeasurementCache textMeasurementCache,
            ITelemetryProvider telemetryProvider,
            AbstractMediaPlayerProvider mediaPlayerProvider,
            IImageLoaderProvider imageLoaderProvider,
            IImageProcessor imageProcessor,
            IImageUriSchemeValidator imageUriSchemeValidator,
            IExtensionImageFilterCallback extensionImageFilterCallback,
            IBitmapFactory bitmapFactory,
            IBitmapCache bitmapCache,
            IContentRetriever<Uri, String> avgRetriever) {
        this.docVersion = docVersion;
        this.metricsTransform = metricsTransform;
        this.textMeasurementCache = textMeasurementCache;
        this.telemetryProvider = telemetryProvider;
        this.mediaPlayerProvider = mediaPlayerProvider;
        this.imageLoaderProvider = imageLoaderProvider;
        this.imageProcessor = imageProcessor;
        this.imageUriSchemeValidator = imageUriSchemeValidator;
        this.extensionImageFilterCallback = extensionImageFilterCallback;
        this.bitmapFactory = bitmapFactory;
        this.bitmapCache = bitmapCache;
        this.avgRetriever = avgRetriever;
    }

    public int getDocVersion() {
        return docVersion;
    }

    public IMetricsTransform getMetricsTransform() {
        return metricsTransform;
    }

    public void setMetricsTransform(IMetricsTransform metricsTransform) {
        this.metricsTransform = metricsTransform;
    }

    public ITextMeasurementCache getTextMeasurementCache() {
        return textMeasurementCache;
    }

    public ITelemetryProvider getTelemetryProvider() {
        return telemetryProvider;
    }

    public AbstractMediaPlayerProvider getMediaPlayerProvider() {
        return mediaPlayerProvider;
    }

    public IImageLoaderProvider getImageLoaderProvider() {
        return imageLoaderProvider;
    }

    public IImageProcessor getImageProcessor() {
        return imageProcessor;
    }

    public IImageUriSchemeValidator getImageUriSchemeValidator() {
        return imageUriSchemeValidator;
    }

    public IExtensionImageFilterCallback getExtensionImageFilterCallback() {
        return extensionImageFilterCallback;
    }

    public IBitmapFactory getBitmapFactory() {
        return bitmapFactory;
    }

    public IBitmapCache getBitmapCache() {
        return bitmapCache;
    }

    public IContentRetriever<Uri, String> getAvgRetriever() {
        return avgRetriever;
    }

    // Defaults are no-ops
    public static Builder builder() {
        return new RenderingContext.Builder()
                .docVersion(APLVersionCodes.APL_1_0)
                .metricsTransform(NoOpMetricsTransform.getInstance())
                .textMeasurementCache(NoOpTextMeasurementCache.getInstance())
                .telemetryProvider(NoOpTelemetryProvider.getInstance())
                .mediaPlayerProvider(NoOpMediaPlayerProvider.getInstance())
                .imageLoaderProvider(NoOpImageLoaderProvider.getInstance())
                .imageProcessor(((sources, bitmaps) -> bitmaps))
                .imageUriSchemeValidator((scheme, version) -> true)
                .extensionImageFilterCallback(((sourceBitmap, destinationBitmap, params) -> sourceBitmap))
                .bitmapFactory(BitmapFactory.create(NoOpTelemetryProvider.getInstance()))
                .bitmapCache(new NoOpBitmapCache());
    }

    public static final class Builder {
        private Integer docVersion;
        private IMetricsTransform metricsTransform;
        private ITextMeasurementCache textMeasurementCache;
        private ITelemetryProvider telemetryProvider;
        private AbstractMediaPlayerProvider mediaPlayerProvider;
        private IImageLoaderProvider imageLoaderProvider;
        private IImageProcessor imageProcessor;
        private IImageUriSchemeValidator imageUriSchemeValidator;
        private IExtensionImageFilterCallback extensionImageFilterCallback;
        private IBitmapFactory bitmapFactory;
        private IBitmapCache bitmapCache;
        private IContentRetriever<Uri, String> avgRetriever;

        Builder() {
        }

        public RenderingContext.Builder docVersion(int docVersion) {
            this.docVersion = docVersion;
            return this;
        }

        public RenderingContext.Builder metricsTransform(IMetricsTransform metricsTransform) {
            this.metricsTransform = metricsTransform;
            return this;
        }

        public RenderingContext.Builder textMeasurementCache(ITextMeasurementCache textMeasurementCache) {
            this.textMeasurementCache = textMeasurementCache;
            return this;
        }

        public RenderingContext.Builder telemetryProvider(ITelemetryProvider telemetryProvider) {
            this.telemetryProvider = telemetryProvider;
            return this;
        }

        public RenderingContext.Builder mediaPlayerProvider(AbstractMediaPlayerProvider mediaPlayerProvider) {
            this.mediaPlayerProvider = mediaPlayerProvider;
            return this;
        }

        public RenderingContext.Builder imageLoaderProvider(IImageLoaderProvider imageLoaderProvider) {
            this.imageLoaderProvider = imageLoaderProvider;
            return this;
        }

        public RenderingContext.Builder imageProcessor(IImageProcessor imageProcessor) {
            this.imageProcessor = imageProcessor;
            return this;
        }

        public RenderingContext.Builder imageUriSchemeValidator(IImageUriSchemeValidator imageUriSchemeValidator) {
            this.imageUriSchemeValidator = imageUriSchemeValidator;
            return this;
        }

        public RenderingContext.Builder extensionImageFilterCallback(IExtensionImageFilterCallback extensionImageFilterCallback) {
            this.extensionImageFilterCallback = extensionImageFilterCallback;
            return this;
        }

        public RenderingContext.Builder bitmapFactory(IBitmapFactory bitmapFactory) {
            this.bitmapFactory = bitmapFactory;
            return this;
        }

        public RenderingContext.Builder bitmapCache(IBitmapCache bitmapCache) {
            this.bitmapCache = bitmapCache;
            return this;
        }

        public RenderingContext.Builder avgContentRetriever(IContentRetriever<Uri, String> avgContentRetriever) {
            this.avgRetriever = avgContentRetriever;
            return this;
        }

        public RenderingContext build() {
            return new RenderingContext(
                    this.docVersion,
                    this.metricsTransform,
                    this.textMeasurementCache,
                    this.telemetryProvider,
                    this.mediaPlayerProvider,
                    this.imageLoaderProvider,
                    this.imageProcessor,
                    this.imageUriSchemeValidator,
                    this.extensionImageFilterCallback,
                    this.bitmapFactory,
                    this.bitmapCache,
                    this.avgRetriever);
        }
    }
}
