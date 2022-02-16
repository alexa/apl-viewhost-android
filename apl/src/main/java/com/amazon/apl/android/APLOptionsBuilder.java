/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;


import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;

import com.amazon.apl.android.dependencies.IDataSourceErrorCallback;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IImageProcessor;
import com.amazon.apl.android.dependencies.IImageUriSchemeValidator;
import com.amazon.apl.android.dependencies.IOnAplFinishCallback;
import com.amazon.apl.android.dependencies.IOpenUrlCallback;
import com.amazon.apl.android.dependencies.IScreenLockListener;
import com.amazon.apl.android.dependencies.ISendEventCallback;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.dependencies.impl.DefaultUriSchemeValidator;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.IDataRetrieverProvider;
import com.amazon.apl.android.providers.IImageLoaderProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.android.providers.impl.GlideImageLoaderProvider;
import com.amazon.apl.android.providers.impl.HttpRetrieverProvider;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTtsPlayerProvider;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.enums.AnimationQuality;

/**
 * @deprecated Please use {@link APLOptions.Builder} instead.
 */
@Deprecated
public class APLOptionsBuilder {
    private ITelemetryProvider mTelemetryProvider;
    private IImageLoaderProvider mImageDownloaderProvider;
    private AbstractMediaPlayerProvider<? extends View> mMediaPlayerProvider;
    private ITtsPlayerProvider mTtsPlayerProvider;
    private IDataRetrieverProvider mDataRetrieverProvider;
    private IImageProcessor mImageProcessor;
    private IVisualContextListener mVisualContextListener;
    private IScreenLockListener mScreenLockListener;
    private IOnAplFinishCallback mOnAplFinishListener;
    private IImageUriSchemeValidator mImageUriSchemeValidator;

    /**
     * OpenURL command callback
     */
    private IOpenUrlCallback mOpenUrlCallback;

    /**
     * SendEvent command callback
     */
    private ISendEventCallback mSendEventCallback;

    /**
     * {@link com.amazon.apl.android.events.DataSourceFetchEvent} callback.
     */
    private IDataSourceFetchCallback mDataSourceFetchCallback;

    /**
     * Callback for Dynamic DataSource errors
     */
    private IDataSourceErrorCallback mDataSourceErrorCallback;

    /**
     * CustomCommand Event handler.
     */
    private IExtensionEventCallback mCustomCommandEventCallback;


    /**
     * @deprecated Please use the {@link APLOptions.Builder#builder()} method and create
     *  {@link RootConfig} directly with {@link RootConfig#create(String, String)}.
     *
     * @param agent   Agent name
     * @param version Agent version
     * @return An instance of a {@link APLOptionsBuilder}
     */
    @Deprecated
    public static APLOptionsBuilder create(@NonNull String agent, @NonNull String version) {
        APLOptionsBuilder builder = create();
        return builder;
    }

    /**
     * Create the default builder.
     *
     * @return an instance of {@link APLOptionsBuilder}
     */
    public static APLOptionsBuilder create() {
        return new APLOptionsBuilder();
    }

    /**
     * Private constructor for APLOptionsBuilder
     */
    private APLOptionsBuilder() { }


    /**
     * Sets up defaults for any option that is unset.
     */
    private void setDefaults() {

        if (mTelemetryProvider == null) {
            mTelemetryProvider = NoOpTelemetryProvider.getInstance();
        }

        if (mImageDownloaderProvider == null) {
            mImageDownloaderProvider = new GlideImageLoaderProvider();
        }

        if (mMediaPlayerProvider == null) {
            mMediaPlayerProvider = new MediaPlayerProvider();
        }

        if (mTtsPlayerProvider == null) {
            mTtsPlayerProvider = new NoOpTtsPlayerProvider();
        }

        if (mDataRetrieverProvider == null) {
            mDataRetrieverProvider = new HttpRetrieverProvider();
        }

        if (mImageProcessor == null) {
            mImageProcessor = (sources, bitmaps) -> bitmaps;
        }

        if (mVisualContextListener == null) {
            mVisualContextListener = visualContext -> {};
        }

        if (mScreenLockListener == null) {
            mScreenLockListener = status -> {};

        }

        if (mOpenUrlCallback == null) {
            mOpenUrlCallback = (url, resultCallback) -> {
                // do nothing. Notify callback a 'false' succeeded result.
                // It requires from the consumer to parse the URL and open a new
                // activity through the activity.
                resultCallback.onResult(false);
            };
        }

        if (mSendEventCallback == null) {
            mSendEventCallback = (args, components, sources) -> {};
        }

        if (mCustomCommandEventCallback == null) {
            mCustomCommandEventCallback = (name, uri, source, custom, resultCallback) -> {};
        }

        if (mOnAplFinishListener == null) {
            mOnAplFinishListener = () -> {};
        }

        if(mDataSourceFetchCallback == null) {
            mDataSourceFetchCallback = (type, payload) -> {};
        }

        if(mDataSourceErrorCallback == null) {
            mDataSourceErrorCallback = (errors) -> {};
        }

        if (mImageUriSchemeValidator == null) {
            mImageUriSchemeValidator = new DefaultUriSchemeValidator();
        }
    }

    /**
     * Sets `environment.allowOpenUrl` in the root context
     *
     * @deprecated Please use {@link RootConfig#allowOpenUrl(boolean)} instead.
     *
     * @param value true to allow
     * @return The builder
     */
    @NonNull
    @Deprecated
    public APLOptionsBuilder allowOpenUrl(boolean value) {
        return this;
    }

    /**
     * Sets `environment.disallowVideo` in the root context
     *
     * @deprecated Please use {@link RootConfig#disallowVideo(boolean)} instead.
     *
     * @param value true to disallow video
     * @return The builder
     */
    @NonNull
    @Deprecated
    public APLOptionsBuilder disallowVideo(boolean value) {
        return this;
    }


    /**
     * Sets the performance telemetry provider.
     *
     * @param telemetryProvider The telemetry provider.
     * @return The builder.
     */
    public APLOptionsBuilder telemetryProvider(@NonNull ITelemetryProvider telemetryProvider) {
        mTelemetryProvider = telemetryProvider;
        return this;
    }

    /**
     * Set `environment.animationQuality` in the root context
     *
     * @deprecated Please use {@link RootConfig#animationQuality(AnimationQuality)} instead.
     *
     * @param quality Animation quality
     * @return The builder
     */
    @NonNull
    @Deprecated
    public APLOptionsBuilder animationQuality(@NonNull AnimationQuality quality) {
        return this;
    }

    /**
     * @deprecated  Please use {@link APLLayout#setScaling(Scaling)} instead.
     * @param scaling the scaling object
     * @return the builder
     */
    @NonNull
    @Deprecated
    public APLOptionsBuilder addScaling(@NonNull Scaling scaling) {
        return this;
    }

    /**
     * @deprecated Please use {@link APLLayout#overrideDpi(int)} instead.
     * @param overrideDpi The override dpi value.
     * @return The builder
     */
    @NonNull
    @Deprecated
    public APLOptionsBuilder addOverrideDpi(@NonNull Integer overrideDpi) {
        return this;
    }

    /**
     * Allows the consumer to set its own Image Provider.
     *
     *
     * @param provider The provider
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder imageProvider(@NonNull IImageLoaderProvider provider) {
        mImageDownloaderProvider = provider;
        return this;
    }

    /**
     * Allows the consumer to set its own TTS Provider.
     *
     * @param provider The provider
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder ttsPlayerProvider(@NonNull ITtsPlayerProvider provider) {
        mTtsPlayerProvider = provider;
        return this;
    }

    /**
     * Allows the consumer to set its own Media Provider.
     *
     * @param provider The provider
     * @return The builder
     */
    @NonNull
    public <T extends View> APLOptionsBuilder mediaPlayerProvider(@NonNull AbstractMediaPlayerProvider<T> provider) {
        mMediaPlayerProvider = provider;
        return this;
    }


    /**
     * Allows the consumer to set its own Data Retriever Provider.
     *
     * @param provider The provider
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder dataRetrieverProvider(@NonNull IDataRetrieverProvider provider) {
        mDataRetrieverProvider = provider;
        return this;
    }


    /**
     * Sets the callback for the OpenURL command.
     *
     * @param callback OpenURL command callback.
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder openUrlCallback(@NonNull IOpenUrlCallback callback) {
        mOpenUrlCallback = callback;
        return this;
    }

    /**
     * Sets the callback for the SendEvent command.
     *
     * @param callback SendEvent callback.
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder sendEventCallback(@NonNull ISendEventCallback callback) {
        mSendEventCallback = callback;
        return this;
    }


    public APLOptionsBuilder customCommandEventCallback(@NonNull IExtensionEventCallback callback) {
        mCustomCommandEventCallback = callback;
        return this;
    }

    /**
     * Sets the visual context handler.
     *
     * @param visualContextListener The visual context handler.
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder visualContextListener(@NonNull IVisualContextListener visualContextListener) {
        mVisualContextListener = visualContextListener;
        return this;
    }

    /**
     * Sets the screen lock handler.
     *
     * @param screenLockListener The screen lock handler.
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder screenLockListener(@NonNull IScreenLockListener screenLockListener) {
        mScreenLockListener = screenLockListener;
        return this;
    }

    /**
     * Sets the onAplFinishListener.
     * <p>
     * This listener is responsible for responding to FinishEvents from the Viewhost.
     *
     * @param onAplFinishListener the finish listener.
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder onAplFinishListener(@NonNull IOnAplFinishCallback onAplFinishListener) {
        mOnAplFinishListener = onAplFinishListener;
        return this;
    }

    /**
     * Sets the callback that will be triggered by DataSource Fetch events
     *
     * @param dataSourceFetchCallback A {@link IDataSourceFetchCallback}
     * @return The builder
     */
    public APLOptionsBuilder dataSourceFetchCallback(@NonNull IDataSourceFetchCallback dataSourceFetchCallback) {
        mDataSourceFetchCallback = dataSourceFetchCallback;
        return this;
    }

    /**
     * Sets the callback that will be triggered by DataSource errors
     *
     * @param dataSourceErrorCallback A {@link IDataSourceErrorCallback}
     * @return The builder
     */
    public APLOptionsBuilder dataSourceErrorCallback(@NonNull IDataSourceErrorCallback dataSourceErrorCallback) {
        mDataSourceErrorCallback = dataSourceErrorCallback;
        return this;
    }

    /*
     * Sets the URI scheme validator to be used for downloading Images.
     * @param uriSchemeValidator the new URL scheme validator.
     * @return The builder
     */
    @NonNull
    public APLOptionsBuilder imageUriSchemeValidator(@NonNull IImageUriSchemeValidator uriSchemeValidator) {
        mImageUriSchemeValidator = uriSchemeValidator;
        return this;
    }

    /**
     * Build the final configuration
     *
     * @return A {@link APLOptions}
     */
    @NonNull
    public APLOptions build() {

        setDefaults();

        return APLOptions.builder()
                .extensionEventCallback(mCustomCommandEventCallback)
                .onAplFinishCallback(mOnAplFinishListener)
                .visualContextListener(mVisualContextListener)
                .ttsPlayerProvider(mTtsPlayerProvider)
                .telemetryProvider(mTelemetryProvider)
                .sendEventCallback(mSendEventCallback)
                .screenLockListener(mScreenLockListener)
                .openUrlCallback(mOpenUrlCallback)
                .mediaPlayerProvider(mMediaPlayerProvider)
                .imageUriSchemeValidator(mImageUriSchemeValidator)
                .imageProvider(mImageDownloaderProvider)
                .imageProcessor(mImageProcessor)
                .dataSourceFetchCallback(mDataSourceFetchCallback)
                .dataSourceErrorCallback(mDataSourceErrorCallback)
                .dataRetrieverProvider(mDataRetrieverProvider)
                .build();
    }

    /**
     * The context was only necessary for the default TtsPlayerProvider which we are removing from this
     * package.
     *
     * @deprecated Please call {@link APLOptionsBuilder#build()} instead
     * @param context
     * @return
     */
    public APLOptions build(Context context) {
        return build();
    }
}