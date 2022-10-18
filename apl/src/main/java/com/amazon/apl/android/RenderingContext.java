/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.net.Uri;

import com.amazon.alexaext.ExtensionResourceProvider;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.bitmap.NoOpBitmapCache;
import com.amazon.apl.android.bitmap.SimpleBitmapFactory;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
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
import com.amazon.apl.android.utils.APLTrace;

/**
 * Contains context needed to render a document. This class serves as a convenient way to pass
 * context from the top of the application (RootContext) down to all levels of the code base.
 */
public class RenderingContext {

    private final int docVersion;
    private IMetricsTransform metricsTransform;
    private final TextLayoutFactory textLayoutFactory;
    private final ITelemetryProvider telemetryProvider;
    private final ExtensionResourceProvider resourceProvider;
    private AbstractMediaPlayerProvider mediaPlayerProvider;
    private final IImageLoaderProvider imageLoaderProvider;
    private final IImageProcessor imageProcessor;
    private final IImageUriSchemeValidator imageUriSchemeValidator;
    private final IExtensionImageFilterCallback extensionImageFilterCallback;
    private final IBitmapFactory bitmapFactory;
    private final IBitmapCache bitmapCache;
    private final IContentRetriever<Uri, String> avgRetriever;
    private final IExtensionEventCallback extensionEventCallback;
    private final APLTrace aplTrace;
    private final boolean mediaPlayerV2Enabled;

    private RenderingContext(
            int docVersion,
            IMetricsTransform metricsTransform,
            TextLayoutFactory textLayoutFactory,
            ITelemetryProvider telemetryProvider,
            ExtensionResourceProvider resourceProvider,
            AbstractMediaPlayerProvider mediaPlayerProvider,
            IImageLoaderProvider imageLoaderProvider,
            IImageProcessor imageProcessor,
            IImageUriSchemeValidator imageUriSchemeValidator,
            IExtensionImageFilterCallback extensionImageFilterCallback,
            IBitmapFactory bitmapFactory,
            IBitmapCache bitmapCache,
            IContentRetriever<Uri, String> avgRetriever,
            IExtensionEventCallback extensionEventCallback,
            APLTrace aplTrace,
            boolean mediaPlayerV2Enabled) {
        this.docVersion = docVersion;
        this.metricsTransform = metricsTransform;
        this.textLayoutFactory = textLayoutFactory;
        this.telemetryProvider = telemetryProvider;
        this.resourceProvider = resourceProvider;
        this.mediaPlayerProvider = mediaPlayerProvider;
        this.imageLoaderProvider = imageLoaderProvider;
        this.imageProcessor = imageProcessor;
        this.imageUriSchemeValidator = imageUriSchemeValidator;
        this.extensionImageFilterCallback = extensionImageFilterCallback;
        this.bitmapFactory = bitmapFactory;
        this.bitmapCache = bitmapCache;
        this.avgRetriever = avgRetriever;
        this.extensionEventCallback = extensionEventCallback;
        this.aplTrace = aplTrace;
        this.mediaPlayerV2Enabled = mediaPlayerV2Enabled;
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

    public TextLayoutFactory getTextLayoutFactory() {
        return textLayoutFactory;
    }

    public ITelemetryProvider getTelemetryProvider() {
        return telemetryProvider;
    }

    public ExtensionResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public AbstractMediaPlayerProvider getMediaPlayerProvider() {
        return mediaPlayerProvider;
    }

    public void setMediaPlayerProvider(AbstractMediaPlayerProvider mediaPlayerProvider) {
        this.mediaPlayerProvider = mediaPlayerProvider;
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

    public IExtensionEventCallback getExtensionEventCallback() { return extensionEventCallback; }

    public APLTrace getAplTrace() { return aplTrace; }

    public boolean isMediaPlayerV2Enabled() { return mediaPlayerV2Enabled; }

    // Defaults are no-ops
    public static Builder builder() {
        return new Builder()
                .docVersion(APLVersionCodes.APL_1_0)
                .metricsTransform(NoOpMetricsTransform.getInstance())
                .textLayoutFactory(TextLayoutFactory.defaultFactory())
                .telemetryProvider(NoOpTelemetryProvider.getInstance())
                .extensionResourceProvider(ExtensionResourceProvider.noOpInstance())
                .mediaPlayerProvider(NoOpMediaPlayerProvider.getInstance())
                .imageLoaderProvider(NoOpImageLoaderProvider.getInstance())
                .imageProcessor(null)
                .imageUriSchemeValidator((scheme, version) -> true)
                .extensionImageFilterCallback(((sourceBitmap, destinationBitmap, params) -> sourceBitmap))
                .bitmapFactory(new SimpleBitmapFactory())
                .bitmapCache(new NoOpBitmapCache());
    }

    public static final class Builder {
        private Integer docVersion;
        private IMetricsTransform metricsTransform;
        private TextLayoutFactory textLayoutFactory;
        private ITelemetryProvider telemetryProvider;
        private ExtensionResourceProvider resourceProvider;
        private AbstractMediaPlayerProvider mediaPlayerProvider;
        private IImageLoaderProvider imageLoaderProvider;
        private IImageProcessor imageProcessor;
        private IImageUriSchemeValidator imageUriSchemeValidator;
        private IExtensionImageFilterCallback extensionImageFilterCallback;
        private IBitmapFactory bitmapFactory;
        private IBitmapCache bitmapCache;
        private IContentRetriever<Uri, String> avgRetriever;
        private IExtensionEventCallback extensionEventCallback;
        private APLTrace aplTrace;
        private boolean isMediaPlayerV2Enabled;

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

        public RenderingContext.Builder textLayoutFactory(TextLayoutFactory textLayoutFactory) {
            this.textLayoutFactory = textLayoutFactory;
            return this;
        }


        public RenderingContext.Builder telemetryProvider(ITelemetryProvider telemetryProvider) {
            this.telemetryProvider = telemetryProvider;
            return this;
        }

        public RenderingContext.Builder extensionResourceProvider(ExtensionResourceProvider resourceProvider) {
            this.resourceProvider = resourceProvider;
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

        public RenderingContext.Builder extensionEventCallback(IExtensionEventCallback extensionEventCallback) {
            this.extensionEventCallback = extensionEventCallback;
            return this;
        }

        public RenderingContext.Builder aplTrace(APLTrace aplTrace) {
            this.aplTrace = aplTrace;
            return this;
        }

        public RenderingContext.Builder isMediaPlayerV2Enabled(boolean isMediaPlayerV2Enabled) {
            this.isMediaPlayerV2Enabled = isMediaPlayerV2Enabled;
            return this;
        }

        public RenderingContext build() {
            return new RenderingContext(
                    this.docVersion,
                    this.metricsTransform,
                    this.textLayoutFactory,
                    this.telemetryProvider,
                    this.resourceProvider,
                    this.mediaPlayerProvider,
                    this.imageLoaderProvider,
                    this.imageProcessor,
                    this.imageUriSchemeValidator,
                    this.extensionImageFilterCallback,
                    this.bitmapFactory,
                    this.bitmapCache,
                    this.avgRetriever,
                    this.extensionEventCallback,
                    this.aplTrace,
                    this.isMediaPlayerV2Enabled);
        }
    }
}
