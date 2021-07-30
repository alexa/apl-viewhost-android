/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.ITelemetryProvider.Type;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.thread.Threading;
import com.amazon.apl.android.utils.SystraceUtils;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The APL Consumer API. Controls the document inflation lifecycle and access
 * to the APL system.
 */
@SuppressWarnings("unused")
public class APLController {
    /**
     * Checked Exception for the public APL api.
     */
    @SuppressWarnings("WeakerAccess")
    public static class APLException extends Exception {
        public APLException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /*
     * Record a failure metric and throws exception. This is the standard
     * way to fail methods in this class.
     */
    private static void fail(ITelemetryProvider telemetryProvider,
                             final String failMetric, Type metricType) {
        final int metricId = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN,
                failMetric, metricType);
        telemetryProvider.fail(metricId);
    }

    private static final class Load implements Callable<Boolean> {

        @Override
        public Boolean call() {
            System.loadLibrary("apl-jni");

            return true;
        }
    }

    private static final String TAG = "APLController";

    @Nullable
    private RootContext mRootContext;

    private static Future<Boolean> sLibraryFuture;
    /**
     * ExecutorService that handles freeing native resources. Calls the APLBinding.doDeletes method once every
     * 2 seconds on a background thread. See {@link APLBinding#doDeletes()}.
     */
    private static ScheduledExecutorService sDeleteService;

    private static TypefaceResolver sTypefaceResolver;

    private static RuntimeConfig sRuntimeConfig;

    /**
     * Initialize the APL system resources.
     * @param context The App context
     * @param runtimeConfig Custom Runtime Configuration
     */
    public static synchronized void initializeAPL(@NonNull final Context context, @NonNull RuntimeConfig runtimeConfig) {
        if (sLibraryFuture != null) return;
        sRuntimeConfig = runtimeConfig;
        sLibraryFuture = Threading.THREAD_POOL_EXECUTOR.submit(new Load());
        sDeleteService = Threading.createScheduledExecutor();
        sDeleteService.scheduleAtFixedRate(APLBinding::doDeletes, 5, 2, TimeUnit.SECONDS);
        // initialize fonts typeface resolver in order to start preloading fonts as soon as possible
        TypefaceResolver.getInstance().initialize(context, runtimeConfig);
    }

    /**
     * Initialize the APL system resources.
     *
     * @deprecated Please use {@link APLController#initializeAPL(Context, RuntimeConfig)}  instead.
     *
     * @param context The App context
     */
    @Deprecated
    public static synchronized void initializeAPL(@NonNull final Context context) {
        // initialize APL system resouces with default runtime config
        initializeAPL(context, RuntimeConfig.builder().build());
    }

    @VisibleForTesting
    public APLController(@NonNull RootContext context,
                         @NonNull APLOptions options,
                         @NonNull IAPLViewPresenter presenter) {
        mRootContext = context;
    }

    /**
     * Render a APL document.
     *
     * @param content    The Content to be rendered.
     * @param options    Configurable dependencies.
     * @param rootConfig The root configuration
     * @param presenter  The view Presenter displaying the document.
     * @return An instance of APLController for use in follow on access to the rendered document.
     * @throws APLException Thrown when the document cannot be rendered.
     */
    @UiThread
    public static APLController renderDocument(@NonNull final Content content,
                                               @NonNull final APLOptions options,
                                               @NonNull final RootConfig rootConfig,
                                               @NonNull final IAPLViewPresenter presenter) throws APLException {
        // starts the render timers
        presenter.preDocumentRender(false);

        if (!isInitialized(options.getTelemetryProvider())) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            throw new APLException("The APLController must be initialized.", new IllegalStateException());
        }
        if (!content.isReady()) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            throw new APLException("Content must be in a ready state..", new IllegalStateException());
        }


        if (BuildConfig.DEBUG) {
            SystraceUtils.startTrace(TAG, "renderDocument");
        }

        try {
            final ViewportMetrics metrics = presenter.createViewportMetrics();
            final RootContext rootContext = RootContext.create(metrics, content, rootConfig, options, presenter);
            for (IDocumentLifecycleListener documentLifecycleListener : options.getDocumentLifecycleListeners()) {
                presenter.addDocumentLifecycleListener(documentLifecycleListener);
            }
            presenter.onDocumentRender(rootContext);
            return new APLController(rootContext, options, presenter);
        } catch (Exception e) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            throw new APLException("Cannot render APL document", e);
        } finally {
            if (BuildConfig.DEBUG) {
                SystraceUtils.endTrace();
            }
        }
    }

    /**
     * Render a APL document.
     *
     * @param content   The Content to be rendered.
     * @param options   Configurable dependencies.
     * @param presenter The view Presenter displaying the document.
     * @return An instance of APLController for use in follow on access to the rendered document.
     * @throws APLException Thrown when the document cannot be rendered.
     * @deprecated Please use {@link APLController#renderDocument(Content, APLOptions, RootConfig, IAPLViewPresenter)} instead.
     */
    @Deprecated
    public static APLController renderDocument(@NonNull final Content content,
                                               @NonNull final APLOptions options,
                                               @NonNull final IAPLViewPresenter presenter) throws APLException {
        RootConfig rootConfig = RootConfig.create();
        return renderDocument(content, options, rootConfig, presenter);
    }

    /**
     * Restores the document from a cached document state.
     *
     * @param presenter     the view presenter
     * @param documentState the document to restore
     * @return an instance of APLController
     * @throws APLException thrown if the document fails to be created
     */
    @UiThread
    public static APLController restoreDocument(@NonNull final DocumentState documentState,
                                                @NonNull final IAPLViewPresenter presenter) throws APLException {
        // starts the render timers
        presenter.preDocumentRender(true);

        final APLOptions options = documentState.getOptions();
        Scaling scaling = documentState.getMetricsTransform().getScaledMetrics().scaling();
        // Render document
        try {
            presenter.setScaling(scaling);
            presenter.createViewportMetrics();
            final RootContext rootContext = RootContext.createFromCachedDocumentState(documentState, presenter);
            if (presenter.getConfigurationChange() != null) {
                rootContext.handleConfigurationChange(presenter.getConfigurationChange());
            }
            for (IDocumentLifecycleListener documentLifecycleListener : options.getDocumentLifecycleListeners()) {
                presenter.addDocumentLifecycleListener(documentLifecycleListener);
            }
            presenter.onDocumentRender(rootContext);
            return new APLController(rootContext, documentState.getOptions(), presenter);
        } catch (Exception e) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RESTORE_DOCUMENT, Type.TIMER);
            throw new APLException("Cannot restore APL document", e);
        }
    }

    @Nullable
    public static RuntimeConfig getRuntimeConfig() {
        return sRuntimeConfig;
    }

    /**
     * Pauses the document's processing but maintains rendered views.
     *
     * Invokes {@link IDocumentLifecycleListener#onDocumentPaused()}.
     */
    @UiThread
    public void pauseDocument() throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("Attempting to pause finished document.");
        }
        mRootContext.pauseDocument();
    }

    /**
     * Resumes document processing.
     *
     * Invokes {@link IDocumentLifecycleListener#onDocumentResumed()}.
     */
    @UiThread
    public void resumeDocument() throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("Attempting to resume finished document.");
        }
        mRootContext.resumeDocument();
    }

    /**
     * @return a document state corresponding to this instance of APLController.
     */
    public DocumentState getDocumentState() throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("Attempting to get document state of finished document.");
        }
        return new DocumentState(mRootContext);
    }

    /**
     * @deprecated use {@link Content#hasSetting(String).}
     */
    public boolean hasSetting(String propertyName) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused.");
        }
        return mRootContext.hasSetting(propertyName);
    }

    /**
     * @deprecated use {@link Content#optSetting(String, Object).}
     */
    public <K> K optSetting(String propertyName, K defaultValue) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused.");
        }
        return mRootContext.optSetting(propertyName, defaultValue);
    }

    /**
     * Execute APL commands against the document.
     *
     * @param commands The Commands to run.
     * @return A Action reference to the commands.
     */
    @Nullable
    public Action executeCommands(@NonNull String commands) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        return mRootContext.executeCommands(commands);
    }


    /**
     * Invoke an extension event handler.
     *
     * @param uri      The URI of the custom document handler
     * @param name     The name of the handler to invoke
     * @param data     The data to associate with the handler
     * @param fastMode If true, this handler will be invoked in fast mode
     * @return An Action.
     */
    @Nullable
    public Action invokeExtensionEventHandler(@NonNull String uri, @NonNull String name,
                                              Map<String, Object> data, boolean fastMode) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        return mRootContext.invokeExtensionEventHandler(uri, name, data, fastMode);
    }


    /**
     * Consumer cancels all currently executing commands.
     */
    public void cancelExecution() throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        mRootContext.cancelExecution();
    }

    /**
     * Updates data source for the document.
     *
     * @param type DataSource type, should be one of the types registered with {@link RootConfig#registerDataSource(String, int)}
     * @param data an incremental data update
     * @return true if data source has been updated successfully
     * @throws IllegalStateException if {@link RootContext} is in invalid state
     */
    public boolean updateDataSource(@NonNull String type, @NonNull String data) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        return mRootContext.updateDataSource(type, data);
    }

    /**
     * End document rendering.
     */
    @UiThread
    public void finishDocument() throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        mRootContext.finishDocument();
        mRootContext = null;
    }

    /**
     * Update the time zone
     */
    public void updateTimeZone() {
        if (mRootContext != null) {
            Calendar now = Calendar.getInstance();
            long offset = now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET);
            mRootContext.setLocalTimeAdjustment(offset);
        }
    }

    /**
     * @return True if the APL system is initialized.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static synchronized boolean isInitialized(final ITelemetryProvider telemetryProvider) {
        try {
            return sLibraryFuture != null && sLibraryFuture.get();
        } catch (final ExecutionException | InterruptedException ex) {
            final int metricId = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN,
                    ITelemetryProvider.LIBRARY_INITIALIZATION_FAILED, Type.COUNTER);
            telemetryProvider.incrementCount(metricId);
            Log.e(TAG, "The shared libraries failed to initialize", ex);
        }
        return false;
    }
}
