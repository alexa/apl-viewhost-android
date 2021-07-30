/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.GlideCachingBitmapPool;
import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.bitmap.LruBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.LruBitmapCache;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IContentDataRetriever;
import com.amazon.apl.android.dependencies.IDataSourceContextListener;
import com.amazon.apl.android.dependencies.IDataSourceErrorCallback;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.dependencies.IOnAplFinishCallback;
import com.amazon.apl.android.dependencies.IOpenUrlCallback;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.dependencies.IScreenLockListener;
import com.amazon.apl.android.dependencies.ISendEventCallback;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.dependencies.impl.DefaultUriSchemeValidator;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetrieverProvider;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.android.providers.impl.GlideImageLoaderProvider;
import com.amazon.apl.android.providers.impl.HttpRetrieverProvider;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTtsPlayerProvider;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

import java.util.Collection;
import java.util.LinkedList;

/**
 * APLOptions are runtime configurable providers, callbacks, and listeners.
 *
 * Some examples of providers are:
 *  1) Telemetry providers for metrics (see {@link ITelemetryProvider})
 *  2) Image providers for image loading (see {@link IImageLoaderProvider})
 *  3) Tts providers for speak events (see {@link ITtsPlayerProvider})
 *
 * Some examples of callbacks are:
 *  1) Finish callback for responding to Finish events (see {@link IOnAplFinishCallback})
 *  2) SendEvent callback for responding to Send events (see {@link ISendEventCallback})
 *  3) DataSourceFetch callbacck for responding to data source fetch requests (see {@link IDataSourceFetchCallback})
 *
 * Some examples of listeners are:
 *  1) VisualContext listener for updating the Alexa visual context (see {@link IVisualContextListener})
 *  2) ScreenLock listener for updating screen lock changes (see {@link IScreenLockListener})
 */
@AutoValue
public abstract class APLOptions {
    // Providers
    public abstract ITelemetryProvider getTelemetryProvider();
    public abstract IImageLoaderProvider getImageProvider();
    public abstract AbstractMediaPlayerProvider getMediaPlayerProvider();
    public abstract ITtsPlayerProvider getTtsPlayerProvider();

    public abstract IImageProcessor getImageProcessor();

    // Callbacks
    public abstract IOnAplFinishCallback getOnAplFinishCallback();
    public abstract IOpenUrlCallback getOpenUrlCallback();
    public abstract ISendEventCallback getSendEventCallback();
    public abstract IExtensionEventCallback getExtensionEventCallback();
    public abstract IDataSourceFetchCallback getDataSourceFetchCallback();
    public abstract IDataSourceErrorCallback getDataSourceErrorCallback();
    public abstract IExtensionImageFilterCallback getExtensionImageFilterCallback();

    // Content Retrievers
    public abstract IPackageLoader getPackageLoader();
    public abstract IContentDataRetriever getContentDataRetriever();
    public abstract IContentRetriever<Uri, String> getAvgRetriever();

    // Listeners
    public abstract IVisualContextListener getVisualContextListener();
    public abstract IScreenLockListener getScreenLockListener();
    public abstract IDataSourceContextListener getDataSourceContextListener();
    // Other
    public abstract IImageUriSchemeValidator getImageUriSchemeValidator();
    public abstract IBitmapCache getBitmapCache();
    public abstract IBitmapPool getBitmapPool();

    /**
     * @return options that are {@link IDocumentLifecycleListener}s.
     */
    @Memoized
    Collection<IDocumentLifecycleListener> getDocumentLifecycleListeners() {
        Collection<IDocumentLifecycleListener> listeners = new LinkedList<>();
        listeners.add(getTelemetryProvider());
        listeners.add(getImageProvider());
        listeners.add(getMediaPlayerProvider());
        listeners.add(getTtsPlayerProvider());
        return listeners;
    }

    /**
     * @return gets a builder for {@link APLOptions} with mostly no-op implementations
     */
    public static Builder builder() {
        return new AutoValue_APLOptions.Builder()
                .dataSourceErrorCallback(errors -> {})
                .dataSourceFetchCallback((type, payload) -> {})
                .extensionEventCallback((name, uri, source, custom, resultCallback) -> {})
                .imageProcessor((sources, bitmaps) -> bitmaps)
                .imageProvider(new GlideImageLoaderProvider())
                .imageUriSchemeValidator(new DefaultUriSchemeValidator())
                .mediaPlayerProvider(new MediaPlayerProvider())
                .onAplFinishCallback(() -> {})
                .extensionImageFilterCallback((sourceBitmap, destinationBitmap, params) -> sourceBitmap)
                .openUrlCallback((url, result)-> result.onResult(false))
                .screenLockListener(status -> {})
                .sendEventCallback((args, components, sources) -> {})
                .telemetryProvider(NoOpTelemetryProvider.getInstance())
                .ttsPlayerProvider(new NoOpTtsPlayerProvider())
                .visualContextListener(visualContext -> {})
                .dataSourceContextListener(dataSourceContext -> {})
                .packageLoader((importRequest, successCallback, failureCallback) -> failureCallback.onFailure(importRequest, "Content package loading not implemented."))
                .contentDataRetriever((request, successCallback, failureCallback) -> failureCallback.onFailure(request, "Content datasources not implemented."))
                .avgRetriever((request, successCallback, failureCallback) -> failureCallback.onFailure(request, "AVG source not implemented."))
                .bitmapCache(new LruBitmapCache())
                .bitmapPool(new GlideCachingBitmapPool(Runtime.getRuntime().maxMemory() / 32));
    }

    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Defaults to {@link LoggingTelemetryProvider}
         * @param provider a telemetry provider
         * @return this builder
         */
        public abstract Builder telemetryProvider(ITelemetryProvider provider);

        /**
         * Defaults to {@link GlideImageLoaderProvider}
         * @param provider an image loader provider
         * @return this builder
         */
        public abstract Builder imageProvider(IImageLoaderProvider provider);

        /**
         * Defaults to {@link MediaPlayerProvider}
         * @param provider a media player provider
         * @return this builder
         */
        public abstract Builder mediaPlayerProvider(AbstractMediaPlayerProvider provider);

        /**
         * Defaults to {@link NoOpTtsPlayerProvider}
         * @param provider a tts player provider
         * @return this builder
         */
        public abstract Builder ttsPlayerProvider(ITtsPlayerProvider provider);

        /**
         * Defaults to no-op.
         *
         * @param provider an image processor provider
         * @return this builder
         */
        public abstract Builder imageProcessor(IImageProcessor provider);

        /**
         * Required to support FinishEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.FinishEvent}
         * @return this builder
         */
        public abstract Builder onAplFinishCallback(IOnAplFinishCallback callback);

        /**
         * Required to support OpenUrlEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.OpenURLEvent}
         * @return this builder
         */
        public abstract Builder openUrlCallback(IOpenUrlCallback callback);

        /**
         * Required to support SendEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.SendEvent}
         * @return this builder
         */
        public abstract Builder sendEventCallback(ISendEventCallback callback);

        /**
         * Required to support ExtensionEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.ExtensionEvent}
         * @return this builder
         */
        public abstract Builder extensionEventCallback(IExtensionEventCallback callback);

        /**
         * Required to support {@link ExtensionFilterDefinition} registered with the {@link RootConfig}.
         *
         * @param callback a callback for ExtensionFilters.
         * @return this builder
         */
        public abstract Builder extensionImageFilterCallback(IExtensionImageFilterCallback callback);

        /**
         * Required to support DataSourceFetchEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.DataSourceFetchEvent}
         * @return this builder
         */
        public abstract Builder dataSourceFetchCallback(IDataSourceFetchCallback callback);

        /**
         * Required to support DataSource errors.
         * Defaults to no-op
         *
         * @param callback a callback for data source errors
         * @return this builder
         */
        public abstract Builder dataSourceErrorCallback(IDataSourceErrorCallback callback);

        /**
         * Required to support VisualContext updates.
         * Defaults to no-op
         *
         * @param callback a callback for VisualContext updates.
         * @return this builder
         */
        public abstract Builder visualContextListener(IVisualContextListener callback);

        /**
         * Required to support dataSourceContext updates.
         * Defaults to no-op
         *
         * @param callback a callback for dataSourceContext updates.
         * @return this builder
         */
        public abstract Builder dataSourceContextListener(IDataSourceContextListener callback);

        /**
         * Required to support ScreenLock change requests.
         * Defaults to no-op
         *
         * @param listener a listener for ScreenLock change requests.
         * @return this builder
         */
        public abstract Builder screenLockListener(IScreenLockListener listener);

        /**
         * Defaults to {@link DefaultUriSchemeValidator}.
         *
         * @param validator a validator for image uris.
         * @return this builder.
         */
        public abstract Builder imageUriSchemeValidator(IImageUriSchemeValidator validator);

        /**
         * Defaults to {@link LruBitmapCache}.
         *
         * Any instance of IBitmapCache that implements {@link android.content.ComponentCallbacks2}
         * will automatically get registered in onDocumentRender and onDocumentFinish
         *
         * @param bitmapCache the cache used to store image bitmaps in
         * @return this builder.
         */
        public abstract Builder bitmapCache(IBitmapCache bitmapCache);

        /**
         * Retriever for document imports.
         * @param packageLoader the package downloader
         * @return this builder.
         */
        public abstract Builder packageLoader(IPackageLoader packageLoader);

        /**
         * Retriever for document data.
         * @param dataRetriever the datasource retriever
         * @return this builder.
         */
        public abstract Builder contentDataRetriever(IContentDataRetriever dataRetriever);

        /**
         * Retriever for avg sources.
         * @param avgRetriever the AVG retriever.
         * @return this builder.
         */
        public abstract Builder avgRetriever(IContentRetriever<Uri, String> avgRetriever);

        /**
         * Defaults to {@link GlideCachingBitmapPool}.
         *
         * @param bitmapPool the pool used to get bitmaps from
         * @return this builder.
         */
        public abstract Builder bitmapPool(IBitmapPool bitmapPool);



        /**
         * Builds the options for this document.
         * @return the {@link APLOptions}
         */
        public abstract APLOptions build();

        /**
         * @deprecated use {@link #avgRetriever(IContentRetriever)}.
         *
         * @param provider a data retriever provider
         * @return this builder
         */
        @Deprecated
        public Builder dataRetrieverProvider(IDataRetrieverProvider provider) {
            avgRetriever((source, successCallback, failureCallback) -> provider.get().fetch(source.toString(), new IDataRetriever.Callback() {
                @Override
                public void success(String response) {
                    successCallback.onSuccess(source, response);
                }

                @Override
                public void error() {
                    failureCallback.onFailure(source, "Error fetching avg source: " + source);
                }
            }));
            return this;
        }
    }
}
