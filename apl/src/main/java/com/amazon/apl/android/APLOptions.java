/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.audio.IAudioPlayerFactory;
import com.amazon.apl.android.dependencies.IContentCompleteCallback;
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
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.dependencies.IViewportSizeUpdateCallback;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.dependencies.impl.DefaultUriSchemeValidator;
import com.amazon.apl.android.dependencies.impl.NoOpUserPerceivedFatalCallback;
import com.amazon.apl.android.extension.IExtensionRegistration;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetrieverProvider;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ILocalTimeOffsetProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.android.providers.impl.GlideImageLoaderProvider;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTtsPlayerProvider;
import com.amazon.apl.viewhost.TimeProvider;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.NoOpEmbeddedDocumentFactory;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
 *  3) DataSourceFetch callback for responding to data source fetch requests (see {@link IDataSourceFetchCallback})
 *
 * Some examples of listeners are:
 *  1) VisualContext listener for updating the Alexa visual context (see {@link IVisualContextListener})
 *  2) ScreenLock listener for updating screen lock changes (see {@link IScreenLockListener})
 */
@AutoValue
public abstract class APLOptions {

    // Providers
    public abstract ITelemetryProvider getTelemetryProvider();

    //Options containing metrics sinks and related information.
    public abstract MetricsOptions getMetricsOptions();
    public abstract IImageLoaderProvider getImageProvider();

    @Nullable
    public abstract EmbeddedDocumentFactory getEmbeddedDocumentFactory();

    @Nullable
    public abstract Viewhost getViewhost();

    /**
     * Get the specified MediaPlayerProvider
     * @return {@link AbstractMediaPlayerProvider}
     * @deprecated Please use {@link RootConfig#getMediaPlayerFactoryProxy()}
     */
    @Deprecated
    public abstract AbstractMediaPlayerProvider getMediaPlayerProvider();

    /**
     * Get the specified TtsPlayerProvider
     * @return {@link ITtsPlayerProvider}
     * @deprecated Please use {@link RootConfig#getAudioPlayerFactoryProxy()}
     */
    @Deprecated
    public abstract ITtsPlayerProvider getTtsPlayerProvider();

    @Nullable
    public abstract IImageProcessor getImageProcessor();

    /**
     *
     * @deprecated use {@link APLOptions#getTimeProvider()} instead.
     */
    @Deprecated //use {@link
    @Nullable
    public abstract ILocalTimeOffsetProvider getLocalTimeOffsetProvider();

    @Nullable
    public abstract TimeProvider getTimeProvider();

    // Callbacks
    public abstract IOnAplFinishCallback getOnAplFinishCallback();
    public abstract IOpenUrlCallback getOpenUrlCallback();
    public abstract ISendEventCallbackV2 getSendEventCallbackV2();
    public abstract IDataSourceFetchCallback getDataSourceFetchCallback();
    public abstract IDataSourceErrorCallback getDataSourceErrorCallback();

    public abstract IViewportSizeUpdateCallback getViewportSizeUpdateCallback();

    /**
     * @deprecated Use {@link com.amazon.alexaext.ExtensionRegistrar}
     */
    @Deprecated
    public abstract IExtensionEventCallback getExtensionEventCallback();

    /**
     * @deprecated Use {@link com.amazon.alexaext.ExtensionRegistrar}
     */
    @Deprecated
    public abstract IExtensionImageFilterCallback getExtensionImageFilterCallback();

    public abstract ExtensionMediator.IExtensionGrantRequestCallback getExtensionGrantRequestCallback();

    public abstract IContentCompleteCallback getContentCompleteCallback();

    public abstract IExtensionRegistration getExtensionRegistration();

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

    public abstract IClockProvider getAplClockProvider();

    public abstract IUserPerceivedFatalCallback getUserPerceivedFatalCallback();

    public abstract boolean isScenegraphEnabled();

    public abstract Map<String, Object> getConfigurationMap();

    /**
     * @return options that are {@link IDocumentLifecycleListener}s.
     */
    @Memoized
    public Collection<IDocumentLifecycleListener> getDocumentLifecycleListeners() {
        Collection<IDocumentLifecycleListener> listeners = new LinkedList<>();
        listeners.add(getTelemetryProvider());
        listeners.add(getImageProvider());
        listeners.add(getMediaPlayerProvider());
        listeners.add(getTtsPlayerProvider());
        if (getMetricsOptions() != null) {
            for (IMetricsSink metricsSink: getMetricsOptions().getMetricsSinkList()) {
                listeners.add(metricsSink);
            }
        }
        return listeners;
    }

    /**
     * @return gets a builder for {@link APLOptions} with mostly no-op implementations
     */
    public static Builder builder() {
        return new AutoValue_APLOptions.Builder()
                .dataSourceErrorCallback(errors -> {})
                .dataSourceFetchCallback((type, payload) -> {})
                .imageProcessor(null)
                .imageProvider(new GlideImageLoaderProvider())
                .imageUriSchemeValidator(new DefaultUriSchemeValidator())
                .mediaPlayerProvider(new MediaPlayerProvider())
                .onAplFinishCallback(() -> {})
                .openUrlCallback((url, result)-> result.onResult(false))
                .extensionEventCallback((name, uri, source, custom, resultCallback) -> {})
                .extensionGrantRequestCallback((uri) -> true)
                .contentCompleteCallback(() -> {})
                .extensionImageFilterCallback((sourceBitmap, destinationBitmap, params) -> sourceBitmap)
                .extensionRegistration(new IExtensionRegistration() {
                    @Override
                    public void registerExtensions(@NonNull Content content, @NonNull RootConfig rootConfig) {}
                })
                .screenLockListener(status -> {})
                .sendEventCallbackV2((args, components, sources, flags) -> {})
                .telemetryProvider(NoOpTelemetryProvider.getInstance())
                .ttsPlayerProvider(new NoOpTtsPlayerProvider())
                .visualContextListener(visualContext -> {})
                .dataSourceContextListener(dataSourceContext -> {})
                .aplClockProvider(callback -> new APLChoreographer(callback))
                .packageLoader((importRequest, successCallback, failureCallback) -> failureCallback.onFailure(importRequest, "Content package loading not implemented."))
                .contentDataRetriever((request, successCallback, failureCallback) -> failureCallback.onFailure(request, "Content datasources not implemented."))
                .avgRetriever((request, successCallback, failureCallback) -> failureCallback.onFailure(request, "AVG source not implemented."))
                .scenegraphEnabled(BuildConfig.BUILD_TYPE.equals("releaseWithSceneGraph"))
                .embeddedDocumentFactory(new NoOpEmbeddedDocumentFactory())
                .viewportSizeUpdateCallback((width, height) ->{})
                .userPerceivedFatalCallback(new NoOpUserPerceivedFatalCallback())
                .configurationMap(new HashMap<String, Object>())
                .metricsOptions(MetricsOptions.builder().metricsSinkList(Collections.emptyList()).build());
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
         * Metrics Options to be used by metrics recorder.
         * @param metricsOptions
         * @return this builder
         */
        public abstract Builder metricsOptions(MetricsOptions metricsOptions);

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
         * @deprecated Please use {@link RootConfig#mediaPlayerFactory(RuntimeMediaPlayerFactory)}
         */
        @Deprecated
        public abstract Builder mediaPlayerProvider(AbstractMediaPlayerProvider provider);

        /**
         * Defaults to {@link NoOpTtsPlayerProvider}
         * @param provider a tts player provider
         * @return this builder
         * @deprecated Please use {@link RootConfig#audioPlayerFactory(IAudioPlayerFactory)}
         */
        @Deprecated
        public abstract Builder ttsPlayerProvider(ITtsPlayerProvider provider);

        /**
         * Defaults to no-op.
         *
         * @param provider an image processor provider
         * @return this builder
         */
        public abstract Builder imageProcessor(IImageProcessor provider);

        /**
         * Required for the local time to transition with timezones or daylight savings.
         * @param provider the local time offset provider.
         * @return this builder
         * @deprecated Please use {@link Builder#timeProvider(TimeProvider)} instead.
         */
        @Deprecated
        public abstract Builder localTimeOffsetProvider(ILocalTimeOffsetProvider provider);

        /**
         * Enabled only from Unified API pathway
         * Required for the local time to transition with timezones or daylight savings.
         * @param provider the local time offset provider.
         * @return this builder
         */
        public abstract Builder timeProvider(TimeProvider provider);
        /**
         *
         *
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
        public abstract Builder sendEventCallbackV2(ISendEventCallbackV2 callback);

        /**
         *
         * @param callback a callback for the scenarios where auto size is triggered.
         * @return this builder
         */
        public abstract Builder viewportSizeUpdateCallback(IViewportSizeUpdateCallback callback);

        /**
         * Required to support SendEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.SendEvent}
         * @return this builder
         */
        public Builder sendEventCallback(ISendEventCallback callback) {
            return sendEventCallbackV2((args, components, sources, flags) -> callback.onSendEvent(args, components, sources));
        }

        /**
         * Required to support ExtensionEvents.
         * Defaults to no-op
         *
         * @param callback a callback for {@link com.amazon.apl.android.events.ExtensionEvent}
         * @return this builder
         *
         * @deprecated use {@link com.amazon.alexaext.ExtensionRegistrar}
         */
        @Deprecated
        public abstract Builder extensionEventCallback(IExtensionEventCallback callback);

        /**
         * Support content complete: callback for handling when all the imports have been successfully downloaded
         *
         * @param callback a callback for {@link com.amazon.apl.android.content}
         * @return this builder
         */
        public abstract Builder contentCompleteCallback(IContentCompleteCallback callback);

        /**
         * Required to support granted extensions.
         * Defaults to true meaning by default all extensions are granted.
         * @param callback a callback for {@link com.amazon.apl.android.ExtensionMediator.IExtensionGrantRequestCallback}
         * @return this builder
         */
        public abstract Builder extensionGrantRequestCallback(ExtensionMediator.IExtensionGrantRequestCallback callback);

        /**
         * Required to support {@link ExtensionFilterDefinition} registered with the {@link RootConfig}.
         *
         * @param callback a callback for ExtensionFilters.
         * @return this builder
         *
         * @deprecated use {@link com.amazon.alexaext.ExtensionRegistrar}
         */
        @Deprecated
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

        public abstract Builder extensionRegistration(IExtensionRegistration registration);

        public abstract Builder aplClockProvider(@NonNull  IClockProvider clockProvider);

        public abstract Builder scenegraphEnabled(boolean enableScenegraph);

        /**
         * Allow runtime to fulfill embedded document requests
         * @param embeddedDocumentFactory handler of embedded document requests
         * @return this builder
         */
        public abstract Builder embeddedDocumentFactory(@NonNull EmbeddedDocumentFactory embeddedDocumentFactory);

        /**
         * Bridge new viewhost abstraction with legacy rendering pathway.
         * @param viewhost
         * @return this builder
         */
        public abstract Builder viewhost(Viewhost viewhost);

        /**
         * Callback for reporting UPF events to Runtime.
         *
         * @param userPerceivedFatalCallback a callback for UPF
         * @return this builder
         */
        public abstract Builder userPerceivedFatalCallback(IUserPerceivedFatalCallback userPerceivedFatalCallback);

        /**
         * Map of options that could be set by Runtimes as per Viewhost specification
         *
         * @param configurationMap key-value map
         * @return this builder
         */
        public abstract Builder configurationMap(Map<String, Object> configurationMap);

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
                    failureCallback.onFailure(source, "Error fetching avg source");
                }
            }));
            return this;
        }
    }
}
