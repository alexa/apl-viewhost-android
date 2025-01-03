/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.MetricsMilestoneConstants;
import com.amazon.apl.android.metrics.impl.MetricsRecorder;
import com.amazon.apl.android.media.MediaManager;
import com.amazon.apl.android.metrics.impl.NoOpMetricsRecorder;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.utils.ConcurrencyUtils;
import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.dependencies.IPackageCache;
import com.amazon.apl.android.ExtensionMediator.ILoadExtensionCallback;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.common.Consumer;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.ITelemetryProvider.Type;
import com.amazon.apl.android.scaling.Scaling;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.thread.Threading;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.FluidityIncidentReporter;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.devtools.DevToolsProvider;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.devtools.util.DependencyContainer;
import com.amazon.apl.devtools.util.IdGenerator;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.internal.APLProperties;
import com.amazon.apl.viewhost.internal.APLProperty;
import com.amazon.common.NativeBinding;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The APL Consumer API. Controls the document inflation lifecycle and access
 * to the APL system.
 */
@SuppressWarnings("unused")
public class APLController implements IDocumentLifecycleListener, IAPLController {
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
        final int metricId = telemetryProvider.createMetricId(APL_DOMAIN,
                failMetric, metricType);
        telemetryProvider.fail(metricId);
    }

    private static long nativeLibraryLoadingStartTime = 0;
    private static long nativeLibraryLoadingEndTime = 0;
    public static final String LIBRARY_INITIALIZATION_TIME = "libraryInitializationTime";

    private static final class Load implements Callable<Boolean> {

        @Override
        public Boolean call() {
            nativeLibraryLoadingStartTime = SystemClock.elapsedRealtime();
            System.loadLibrary("common-jni");

            // In size minimized variant, JNI code of discovery module, which really is JNI code for
            // extension registration and proxy, is merged into apl-jni and JNI shared library
            // for discovery is absent in discovery aar. This is controlled by BuildConfig build type.
            // Since we have two valid variant combinations for our customers viz
            // (apl-release, discovery-standard-release) and (aplMinSized-release, discovery-standard-releaseMinSized);
            // we can use build type of apl library to decide to load discovery JNI or not.
            // if(!BuildConfig.BUILD_TYPE.equals("releaseMinSized")) {
            //     System.loadLibrary("discovery-jni");
            // }

            System.loadLibrary("apl-jni");
            nativeLibraryLoadingEndTime = SystemClock.elapsedRealtime();
            return true;
        }
    }

    private static final String TAG = "APLController";

    private Content mContent;
    private RootContext mRootContext;

    private static Future<Boolean> sLibraryFuture;
    /**
     * ExecutorService that handles freeing native resources. Calls the APLBinding.doDeletes method once every
     * 2 seconds on a background thread. See {@link NativeBinding#doDeletes()}.
     */
    private static ScheduledExecutorService sDeleteService;

    private static TypefaceResolver sTypefaceResolver;

    private static RuntimeConfig sRuntimeConfig;
    // Exposed for mocking
    private final IContentCreator mContentCreator;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Queue<Consumer<RootContext>> mRunnableQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean mIsFinished = new AtomicBoolean(false);
    private final AtomicBoolean mIsDisplayed = new AtomicBoolean(false);
    private Integer mRenderDocumentTimer;
    private static IdGenerator mSerializedIdGenerator = new IdGenerator();
    private final int mSerializedDocId;
    private DocumentState mDocumentState;

    /**
     * Start the initialization of the APL system resources. To check whether APL initialization has completed/was successful,
     * call {@code waitForInitializeAPLToComplete}
     * @param context The App context
     * @param runtimeConfig Custom Runtime Configuration
     */
    public static synchronized void initializeAPL(@NonNull final Context context, @NonNull RuntimeConfig runtimeConfig) {
        if (sLibraryFuture != null) return;
        sRuntimeConfig = runtimeConfig;
        sLibraryFuture = Threading.THREAD_POOL_EXECUTOR.submit(new Load());
        sDeleteService = Threading.createScheduledExecutor();
        sDeleteService.scheduleAtFixedRate(NativeBinding::doDeletes, 5, 2, TimeUnit.SECONDS);
        // initialize fonts typeface resolver in order to start preloading fonts as soon as possible
        TypefaceResolver.getInstance().initialize(context, runtimeConfig);
        IPackageCache packageCache = runtimeConfig.getPackageCache();
        if (packageCache != null) {
            context.registerComponentCallbacks(runtimeConfig.getPackageCache());
        }

        IBitmapCache bitmapCache = runtimeConfig.getBitmapCache();
        if (bitmapCache instanceof ComponentCallbacks2) {
            context.registerComponentCallbacks((ComponentCallbacks2) bitmapCache);
        }
        DependencyContainer.getInstance().getTargetCatalog().setAppContext(context);
    }

    /**
     * Blocks on APL initialization. Must be preceded by a call to {@code initializeAPL}.
     * @param telemetryProvider if provided, used to record metrics about AP
     * @return whether APL was successfully initialized
     */
    public static synchronized boolean waitForInitializeAPLToComplete(@Nullable ITelemetryProvider telemetryProvider) {
        ITelemetryProvider nonNullTelemetryProvider = telemetryProvider != null ? telemetryProvider : NoOpTelemetryProvider.getInstance();
        return isInitialized(nonNullTelemetryProvider);
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
        // initialize APL system resources with default runtime config
        initializeAPL(context, RuntimeConfig.builder().build());
    }

    @VisibleForTesting
    public static void setRuntimeConfig(@NonNull RuntimeConfig runtimeConfig) {
        sRuntimeConfig = runtimeConfig;
    }

    @VisibleForTesting
    static void setLibraryFuture(@NonNull Future<Boolean> libraryFuture) {
        sLibraryFuture = libraryFuture;
    }

    /**
     * Private constructor.
     */
    private APLController(IContentCreator contentCreator, int serializedDocId) {
        mContentCreator = contentCreator;
        mSerializedDocId = serializedDocId;
    }

    @VisibleForTesting
    public APLController(RootContext rootContext, Content content, int serializedDocId) {
        this(Content::create, serializedDocId);
        mRootContext = rootContext;
        mContent = content;
    }

    @VisibleForTesting
    interface IContentCreator {
        Content create(String aplDocument, APLOptions options, Content.CallbackV2 callbackV2, RootConfig rootConfig, IDTNetworkRequestHandler dtNetworkRequestHandler);
    }

    private void initializeDocumentManager(@NonNull final RootConfig rootConfig,
                                           @NonNull EmbeddedDocumentFactory embeddedDocumentFactory,
                                           @NonNull ITelemetryProvider telemetryProvider) {
        rootConfig.setDocumentManager(embeddedDocumentFactory, mMainHandler, telemetryProvider);
    }

    /**
     * Internal constructor for an APLController.
     *
     * This handles preparing the content and callbacks to the runtime for registering extensions.
     *
     * @param aplDocument   the apl document json as a String
     * @param options       the apl options for this document
     * @param rootConfig    the RootConfig for this document
     * @param aplLayout     the APLLayout for rendering into
     * @param startTime     the start time for metrics
     * @param errorCallback the error callback for handling errors during inflation.
     * @return              an APLController for interacting with the document.
     */
    static APLController renderDocument(
            @NonNull final String aplDocument,
            @NonNull final APLOptions options,
            @NonNull final RootConfig rootConfig,
            @NonNull final APLLayout aplLayout,
            @NonNull final IContentCreator contentCreator,
            final long startTime,
            @NonNull final InflationErrorCallback errorCallback,
            final boolean disableAsyncInflate,
            @NonNull final DocumentSession documentSession) {
        final int serializedDocId = mSerializedIdGenerator.generateId();
        final DevToolsProvider devToolsProvider = ((APLLayout.APLViewPresenterImpl)aplLayout.getPresenter()).getDevToolsProvider();
        devToolsProvider.getDTView().setCurrentDocumentId(serializedDocId);

        Session aplSession = rootConfig.getSession();
        if (options.getMetricsOptions() != null) {
            devToolsProvider.registerSink(options.getMetricsOptions().getMetricsSinkList());
        }
        aplSession.setAPLListener(devToolsProvider.getAPLSessionListener());
        aplLayout.setAPLSession(aplSession);
        aplSession.write(Session.LogEntryLevel.INFO, Session.LogEntrySource.VIEW, "----- Rendering Document -----");

        final ExtensionRegistrar registrar = rootConfig.getExtensionProvider();
        final ExtensionMediator mediator = (registrar != null) ? ExtensionMediator.create(registrar, documentSession) : null;
        final UserPerceivedFatalReporter upfReporter = new UserPerceivedFatalReporter(options.getUserPerceivedFatalCallback());

        if (mediator != null) {
            rootConfig.extensionMediator(mediator);
        }

        aplLayout.setAgentName(rootConfig);
        final APLController aplController = new APLController(contentCreator, serializedDocId);

        final ITelemetryProvider telemetryProvider = options.getTelemetryProvider();
        final FluidityIncidentReporter fluidityIncidentCounter = createFluidityIncidentCounter(options, aplLayout.getContext());
        if(options.getEmbeddedDocumentFactory() != null) {
            aplController.initializeDocumentManager(rootConfig, options.getEmbeddedDocumentFactory(), telemetryProvider);
        }
         if (!isInitialized(telemetryProvider)) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            aplController.onDocumentFinish();
            errorCallback.onError(new APLException("The APLController must be initialized.", new IllegalStateException()));
            upfReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.APL_INITIALIZATION_FAILURE);
        }

        if (options.isScenegraphEnabled()) {
            MediaManager mediaManager = new MediaManager(aplController, options.getImageProvider().get(aplLayout.getContext()));
            rootConfig.mediaManager(mediaManager);
        }
        
        if(rootConfig.getPackageManager() == null) {
            PackageManager packageManager = new PackageManager(options.getPackageLoader(), options.getTelemetryProvider(), devToolsProvider.getNetworkRequestHandler());
            rootConfig.packageManager(packageManager);
        }

        aplController.mRenderDocumentTimer = telemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, TAG + "." + ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
        long seed = SystemClock.elapsedRealtime() - startTime;
        telemetryProvider.startTimer(aplController.mRenderDocumentTimer, TimeUnit.MILLISECONDS, seed);

        final IAPLViewPresenter presenter = aplLayout.getPresenter();
        presenter.addDocumentLifecycleListener(aplController);
        for (IDocumentLifecycleListener documentLifecycleListener : rootConfig.getDocumentLifecycleListeners()) {
            if (documentLifecycleListener != null) {
                presenter.addDocumentLifecycleListener(documentLifecycleListener);
            }
        }
        for (IDocumentLifecycleListener documentLifecycleListener : options.getDocumentLifecycleListeners()) {
            presenter.addDocumentLifecycleListener(documentLifecycleListener);
        }

        final Content content = aplController.mContentCreator.create(aplDocument, options, new Content.CallbackV2() {
            @Override
            public void onComplete(Content content) {
                if (aplController.mIsFinished.get()) {
                    Log.i(TAG, "Finished while Content is being prepared. Aborting");
                    return;
                }

                options.getContentCompleteCallback().onComplete();

                if (mediator == null || registrar == null) {
                    // TODO: Going with old APIs. Not recommended. Planned to be removed in future.
                    options.getExtensionRegistration().registerExtensions(content, rootConfig, () -> aplLayout.addMetricsReadyListener(viewportMetrics -> {
                        if (aplController.mIsFinished.get()) {
                            Log.i(TAG, "Finished while APLLayout is being measured. Aborting");
                            return;
                        }

                        final Runnable runnable = () -> {
                            try {
                                content.getMetricsRecorder().recordMilestone(MetricsMilestoneConstants.RENDER_START_MILESTONE);
                                final RootContext rootContext = RootContext.create(viewportMetrics, content, rootConfig, options, presenter, upfReporter, content.getMetricsRecorder(), fluidityIncidentCounter);

                                if (aplController.mIsFinished.get()) {
                                    Log.i(TAG, "Finished while creating RootContext. Aborting");
                                    return;
                                }
                                presenter.onDocumentRender(rootContext);

                            } catch (Exception e) {
                                upfReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.ROOT_CONTEXT_CREATION_FAILURE);
                                handleRenderingError(e, aplController, telemetryProvider, errorCallback, aplSession, content.getMetricsRecorder());
                            }
                        };

                        if (disableAsyncInflate) {
                            runnable.run();
                        } else {
                            Threading.THREAD_POOL_EXECUTOR.submit(runnable);
                        }
                    }));
                } else {
                    // Start extension registration timer
                    final int extensionRegistrationMetric = telemetryProvider.createMetricId(APL_DOMAIN, ITelemetryProvider.METRIC_TIMER_EXTENSION_REGISTRATION, ITelemetryProvider.Type.TIMER);

                    content.getMetricsRecorder().recordMilestone(MetricsMilestoneConstants.EXTENSION_REGISTRATION_START_MILESTONE);
                    telemetryProvider.startTimer(extensionRegistrationMetric);

                    // Give runtime chance to perform any extension related settings.
                    options.getExtensionRegistration().registerExtensions(content, rootConfig);

                    // Initialize any leftover extensions after packages loaded
                    mediator.initializeExtensions(rootConfig, content, options.getExtensionGrantRequestCallback());

                    // Process rendering after extension loaded
                    mediator.loadExtensions(
                            rootConfig,
                            content,
                            new ILoadExtensionCallback() {
                                @Override
                                public Runnable onSuccess() {
                                    return () -> {
                                        mediator.registerImageFilters(registrar, content, rootConfig);
                                        telemetryProvider.stopTimer(extensionRegistrationMetric);
                                        content.getMetricsRecorder().recordMilestone(MetricsMilestoneConstants.EXTENSION_REGISTRATION_END_MILESTONE);
                                        aplLayout.addMetricsReadyListener(viewportMetrics -> {
                                            if (aplController.mIsFinished.get()) {
                                                Log.i(TAG, "Finished while APLLayout is being measured. Aborting");
                                                return;
                                            }

                                            final Runnable runnable = () -> {
                                                try {
                                                    content.getMetricsRecorder().recordMilestone(MetricsMilestoneConstants.RENDER_START_MILESTONE);
                                                    final RootContext rootContext = RootContext.create(viewportMetrics, content, rootConfig, options, presenter, upfReporter, content.getMetricsRecorder(), fluidityIncidentCounter);

                                                    if (aplController.mIsFinished.get()) {
                                                        Log.i(TAG, "Finished while creating RootContext. Aborting");
                                                        return;
                                                    }
                                                    presenter.onDocumentRender(rootContext);
                                                } catch (Exception e) {
                                                    upfReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.ROOT_CONTEXT_CREATION_FAILURE);
                                                    handleRenderingError(e, aplController, telemetryProvider, errorCallback, aplSession, content.getMetricsRecorder());
                                                }
                                            };

                                            if (disableAsyncInflate) {
                                                runnable.run();
                                            } else {
                                                // Temporarily stop processing extension events to
                                                // prevent conflicting with async inflation.
                                                mediator.pauseForInflation();
                                                // Queue async inflation from the main thread to
                                                // ensure that any already-posted extension messages
                                                // are processed first.
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        Threading.THREAD_POOL_EXECUTOR.submit(runnable));
                                            }
                                        });
                                    };
                                }

                                @Override
                                public Runnable onFailure() {
                                    return () -> {
                                        telemetryProvider.fail(extensionRegistrationMetric);
                                        content.getMetricsRecorder().recordMilestone(MetricsMilestoneConstants.EXTENSION_REGISTRATION_FAILED_MILESTONE);
                                        upfReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE);
                                        handleRenderingError(
                                                new APLException("Required extensions failed to load.", new IllegalStateException()),
                                                aplController,
                                                telemetryProvider,
                                                errorCallback,
                                                aplSession,
                                                content.getMetricsRecorder()
                                        );
                                    };
                                }
                            }
                    );
                }
            }

            @Override
            public void onError(Exception e) {
                upfReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.CONTENT_CREATION_FAILURE);
                //this is the case where content is not initialised hence we will create a metrics recorder to propagate the error
                // TODO: Remove the NoOpMetricsRecorder when completely moving to the MetricsRecorder
                IMetricsRecorder metricsRecorder = BuildConfig.DEBUG ? new MetricsRecorder() : new NoOpMetricsRecorder();
                if (options.getMetricsOptions() != null) {
                    List<IMetricsSink> metricsSinkList = options.getMetricsOptions().getMetricsSinkList() == null ?
                            Collections.emptyList() : options.getMetricsOptions().getMetricsSinkList();
                    for (IMetricsSink sink: metricsSinkList) {
                        metricsRecorder.addSink(sink);
                    }
                    metricsRecorder.mergeMetadata(options.getMetricsOptions().getMetaData());
                }
                handleRenderingError(e, aplController, telemetryProvider, errorCallback, aplSession, metricsRecorder);
            }

            @Override
            public void onPackageLoaded(Content content) {
                if (aplController.mIsFinished.get()) {
                    Log.i(TAG, "Finished while Content is being prepared. Not registering extensions.");
                    return;
                }

                if (mediator != null) {
                    // initialize any requested extensions
                    mediator.initializeExtensions(rootConfig, content, options.getExtensionGrantRequestCallback());
                    mediator.registerImageFilters(registrar, content, rootConfig);
                }
            }
        }, rootConfig, devToolsProvider.getNetworkRequestHandler());
        if (content != null) {
            aplController.mContent = content;
            aplLayout.addMetricsReadyListener(viewportMetrics -> {
                if (aplController.mIsFinished.get()) {
                    Log.i(TAG, "Finished while APLLayout is being measured. Not rendering background.");
                    return;
                }

                content.createDocumentBackground(viewportMetrics, rootConfig);
                presenter.loadBackground(content.getDocumentBackground());
            });
        }

        documentSession.bind(aplController);
        documentSession.onSessionEnded(session -> aplController.executeOnCoreThread(() -> {
            if (mediator != null) {
                mediator.onSessionEnded();
            }
        }));

        long libraryInitializationTime = nativeLibraryLoadingEndTime - nativeLibraryLoadingStartTime;
        if (libraryInitializationTime > 0) {
            int metricId = telemetryProvider.createMetricId(APL_DOMAIN, LIBRARY_INITIALIZATION_TIME, Type.TIMER);
            telemetryProvider.reportTimer(metricId, TimeUnit.MILLISECONDS, libraryInitializationTime);
        } else {
            Log.e(TAG, "Library initialization time will not be recorded due to failure");
        }
        return aplController;
    }

    private static void handleRenderingError(Exception e, APLController aplController, ITelemetryProvider telemetryProvider,
                                             InflationErrorCallback errorCallback, Session aplSession, IMetricsRecorder metricsRecorder) {
        telemetryProvider.fail(aplController.mRenderDocumentTimer);
        metricsRecorder.recordMilestone(MetricsMilestoneConstants.RENDER_FAILED_MILESTONE);
        aplController.onDocumentFinish();
        errorCallback.onError(e);
        aplSession.write(Session.LogEntryLevel.ERROR, Session.LogEntrySource.VIEW, "Document Failed: " + e.getMessage());
    }

    /**
     * @deprecated Please use the {@link Builder} instead.
     *
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
    @Deprecated
    public static APLController renderDocument(
            @NonNull final Content content,
            @NonNull final APLOptions options,
            @NonNull final RootConfig rootConfig,
            @NonNull final IAPLViewPresenter presenter) throws APLException {
        IMetricsRecorder metricsRecorder = content.getMetricsRecorder();
        if (!isInitialized(options.getTelemetryProvider())) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            metricsRecorder.recordMilestone(MetricsMilestoneConstants.RENDER_FAILED_MILESTONE);
            throw new APLException("The APLController must be initialized.", new IllegalStateException());
        }
        if (!content.isReady()) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            metricsRecorder.recordMilestone(MetricsMilestoneConstants.RENDER_FAILED_MILESTONE);
            throw new APLException("Content must be in a ready state..", new IllegalStateException());
        }

        try (APLTrace.AutoTrace autoTrace = presenter.getAPLTrace().startAutoTrace(TracePoint.APL_CONTROLLER_RENDER_DOCUMENT)) {
            final ViewportMetrics metrics = presenter.getOrCreateViewportMetrics();
            content.getMetricsRecorder().recordMilestone(MetricsMilestoneConstants.RENDER_START_MILESTONE);
            final FluidityIncidentReporter fluidityIncidentCounter = createFluidityIncidentCounter(options, presenter.getContext());
            final RootContext rootContext = RootContext.create(metrics, content, rootConfig, options, presenter, metricsRecorder, fluidityIncidentCounter);
            for (IDocumentLifecycleListener documentLifecycleListener : options.getDocumentLifecycleListeners()) {
                presenter.addDocumentLifecycleListener(documentLifecycleListener);
            }
            APLController aplController = new APLController(rootContext, content, mSerializedIdGenerator.generateId());
            presenter.addDocumentLifecycleListener(aplController);
            presenter.onDocumentRender(rootContext);

            return aplController;
        } catch (Exception e) {
            fail(options.getTelemetryProvider(), ITelemetryProvider.RENDER_DOCUMENT, Type.TIMER);
            metricsRecorder.recordMilestone(MetricsMilestoneConstants.RENDER_FAILED_MILESTONE);
            throw new APLException("Cannot render APL document", e);
        }
    }

    /**
     * @deprecated Please use the {@link Builder} instead.
     *
     * Render a APL document.
     *
     * @param content   The Content to be rendered.
     * @param options   Configurable dependencies.
     * @param presenter The view Presenter displaying the document.
     * @return An instance of APLController for use in follow on access to the rendered document.
     * @throws APLException Thrown when the document cannot be rendered.
     */
    @UiThread
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
        final APLOptions options = documentState.getOptions();
        final RootConfig rootConfig = documentState.getRootConfig();
        Scaling scaling = documentState.getMetricsTransform().getScaledMetrics().scaling();
        if (presenter instanceof APLLayout.APLViewPresenterImpl) {
            final DevToolsProvider provider = ((APLLayout.APLViewPresenterImpl) presenter).getDevToolsProvider();
            provider.getDTView().setCurrentDocumentId(documentState.getSerializedDocId());
        }
        // Render document
        try {
            presenter.setScaling(scaling);
            presenter.getOrCreateViewportMetrics();
            final FluidityIncidentReporter fluidityIncidentCounter = createFluidityIncidentCounter(options, presenter.getContext());
            final RootContext rootContext = RootContext.createFromCachedDocumentState(documentState, presenter, fluidityIncidentCounter);
            if (presenter.getConfigurationChange() != null) {
                rootContext.handleConfigurationChange(presenter.getConfigurationChange());
            }
            for (IDocumentLifecycleListener documentLifecycleListener : options.getDocumentLifecycleListeners()) {
                presenter.addDocumentLifecycleListener(documentLifecycleListener);
            }
            for (IDocumentLifecycleListener documentLifecycleListener : rootConfig.getDocumentLifecycleListeners()) {
                if (documentLifecycleListener != null) {
                    presenter.addDocumentLifecycleListener(documentLifecycleListener);
                }
            }
            APLController aplController = new APLController(rootContext, documentState.getContent(), documentState.getSerializedDocId());
            presenter.addDocumentLifecycleListener(aplController);
            presenter.onDocumentRender(rootContext);
            ExtensionMediator mediator = rootContext.getRootConfig().getExtensionMediator();
            if (mediator != null) {
                mediator.enable(true);
            }
            return aplController;
        } catch (Exception e) {
            throw new APLException("Cannot restore APL document", e);
        }
    }

    @NonNull
    public static RuntimeConfig getRuntimeConfig() {
        return sRuntimeConfig;
    }

    /**
     * Inform document about its display state
     * @param displayState the {@link DisplayState}
     */
    @Override
    public void updateDisplayState(final DisplayState displayState) {
        executeIfNotFinishedOnMyThread(rootContext -> rootContext.updateDisplayState(displayState));
    }

    /**
     * Pauses the document's processing but maintains rendered views.
     *
     * Invokes {@link IDocumentLifecycleListener#onDocumentPaused()}.
     */
    @Override
    public void pauseDocument() {
        executeIfNotFinishedOnMyThread(RootContext::pauseDocument);
    }

    /**
     * Resumes document processing.
     *
     * Invokes {@link IDocumentLifecycleListener#onDocumentResumed()}.
     */
    @Override
    public void resumeDocument() {
        executeIfNotFinishedOnMyThread(RootContext::resumeDocument);
    }

    @Override
    public void executeOnCoreThread(Runnable task) {
        executeOnMyThread(rootContext -> task.run());
    }

    /**
     * @deprecated the RootContext should be passed directly to the BackExtension.
     *
     * @return a document state corresponding to this instance of APLController.
     */
    @Deprecated
    public DocumentState getDocumentState() throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("Attempting to get document state of finished document.");
        }

        if (mDocumentState == null) {
            mDocumentState = new DocumentState(mRootContext, mContent, mSerializedDocId);
        }

        return mDocumentState;
    }

    /**
     * Return if a setting is present in the main template. See https://aplspec.aka.corp.amazon.com/release-1.7/html/apl_document.html#settings
     * @param propertyName  the property name
     * @return              if the setting is present.
     */
    @Override
    public boolean hasSetting(String propertyName) {
        return mContent.hasSetting(propertyName);
    }

    /**
     * Return a setting from the main template. See https://aplspec.aka.corp.amazon.com/release-1.7/html/apl_document.html#settings
     * @param propertyName  the property name
     * @param defaultValue  the fallback if not present
     * @return              the value if present otherwise the fallback.
     */
    @Override
    public <K> K optSetting(String propertyName, K defaultValue) {
        return mContent.optSetting(propertyName, defaultValue);
    }

    /**
     * @deprecated Use {@link #executeCommands(String, ExecuteCommandsCallback)} to ensure thread safety
     * and proper enqueueing of commands.
     *
     * Execute APL commands against the document.
     *
     * @param commands The Commands to run.
     * @return A Action reference to the commands.
     */
    @Nullable
    @Deprecated
    public Action executeCommands(@NonNull String commands) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        return mRootContext.executeCommands(commands);
    }

    /**
     * Execute APL commands against the document.
     *
     * @param commands  the Commands to run.
     * @param callback  the callback to receive an Action reference if the command doesn't resolve instantly.
     */
    @Override
    public void executeCommands(@NonNull String commands, @Nullable ExecuteCommandsCallback callback) {
        executeIfNotFinishedOnMyThread(rootContext -> {
            Action result = rootContext.executeCommands(commands);
            if (callback != null) {
                callback.onExecuteCommands(result);
            }
        });
    }


    /**
     * @deprecated Use {@link #invokeExtensionEventHandler(String, String, Map, boolean, ExtensionEventHandlerCallback)} to ensure thread safety
     * and proper enqueueing of commands.
     *
     * Invoke an extension event handler.
     *
     * @param uri      The URI of the custom document handler
     * @param name     The name of the handler to invoke
     * @param data     The data to associate with the handler
     * @param fastMode If true, this handler will be invoked in fast mode
     * @return An Action.
     */
    @Nullable
    @Deprecated
    @Override
    public Action invokeExtensionEventHandler(@NonNull String uri, @NonNull String name,
                                              Map<String, Object> data, boolean fastMode) throws IllegalStateException {
        if (mRootContext == null) {
            throw new IllegalStateException("This document is finished and cannot be reused");
        }

        return mRootContext.invokeExtensionEventHandler(uri, name, data, fastMode);
    }

    /**
     * Invoke an extension event handler.
     *
     * @param uri      The URI of the custom document handler
     * @param name     The name of the handler to invoke
     * @param data     The data to associate with the handler
     * @param fastMode If true, this handler will be invoked in fast mode
     * @param callback  the callback to receive an Action reference if the command doesn't resolve instantly.
     */
    @Override
    public void invokeExtensionEventHandler(@NonNull String uri, @NonNull String name,
                                              Map<String, Object> data, boolean fastMode, @Nullable ExtensionEventHandlerCallback callback) {
        executeIfNotFinishedOnMyThread(rootContext -> {
            Action result = rootContext.invokeExtensionEventHandler(uri, name, data, fastMode);
            if (callback != null && result != null) {
                callback.onExtensionEventInvoked(result);
            }
        });
    }


    /**
     * Consumer cancels all currently executing commands.
     */
    @Override
    public void cancelExecution() {
        executeIfNotFinishedOnMyThread(RootContext::cancelExecution);
    }

    /**
     * @deprecated Use {@link #updateDataSource(String, String, UpdateDataSourceCallback)} to ensure thread safety
     * and proper enqueueing of commands.
     * Updates data source for the document.
     *
     * @param type DataSource type, should be one of the types registered with {@link RootConfig#registerDataSource(String)}
     * @param data an incremental data update
     * @return true if data source has been updated successfully
     */
    @Deprecated
    public boolean updateDataSource(@NonNull String type, @NonNull String data) {
        executeIfNotFinishedOnMyThread(rootContext -> rootContext.updateDataSource(type, data));
        return true;
    }

    /**
     * Updates data source for the document.
     *
     * @param type DataSource type, should be one of the types registered with {@link RootConfig#registerDataSource(String)}
     * @param data an incremental data update
     * @param callback a callback to indicate success or failure of the
     * @throws IllegalStateException if {@link RootContext} is in invalid state
     */
    @Override
    public void updateDataSource(@NonNull String type, @NonNull String data, @Nullable UpdateDataSourceCallback callback) {
        executeIfNotFinishedOnMyThread(rootContext -> {
            boolean result = rootContext.updateDataSource(type, data);
            if (callback != null) {
                callback.onDataSourceUpdate(result);
            }
        });
    }

    /**
     * End document rendering.
     */
    @Override
    public void finishDocument() {
        boolean wasFinished = mIsFinished.getAndSet(true);
        mRunnableQueue.clear();
        if (wasFinished) {
            Log.e(TAG, "Already finished the document! Ignoring...");
            return;
        }

        executeOnMyThread(this::finishDocumentInternal);
    }

    /**
     * @return true if document is finished, false otherwise.
     */
    @Override
    public boolean isFinished() {
        return mIsFinished.get();
    }

    @MainThread
    private void finishDocumentInternal(RootContext rootContext) {
        if (rootContext == null) {
            Log.i(TAG, "Canceling inflation.");
            return;
        }

        ExtensionMediator mediator = rootContext.getRootConfig().getExtensionMediator();

        // Cache it, it gets nullified in the rootContext.finishDocument call.
        DocumentState documentState = mDocumentState;

        rootContext.finishDocument();

        if (mediator != null) {
            mediator.enable(false);

            // We reuse the mediator when/if the document is popped off the backstack
            // So we need to ensure we don't finish it.
            if (documentState == null || !documentState.isOnBackStack()) {
                Log.i(TAG, "ExtensionMediator finished.");
                mediator.finish();
            } else {
                Log.i(TAG, "Skipping ExtensionMediator finished due to backstack.");
            }
        }
        ExtensionRegistrar registrar = rootContext.getRootConfig().getExtensionProvider();
        if (registrar != null) {
            //closing on V1 connections since V1 connections are created per document
            // however V2 connections are shared and cannot be closed upfront.
            registrar.closeAllRemoteV1Connections();
        }
    }

    private void executeOnMyThread(final Consumer<RootContext> rootContextFunction) {
        if (Looper.myLooper() == mMainHandler.getLooper()) {
            rootContextFunction.accept(mRootContext);
        } else {
            mMainHandler.post(() -> rootContextFunction.accept(mRootContext));
        }
    }

    private void executeIfNotFinishedOnMyThread(final Consumer<RootContext> rootContextFunction) {
        executeOnMyThread(rootContext -> executeIfNotFinished(rootContext, rootContextFunction));
    }

    @MainThread
    private void executeIfNotFinished(RootContext rootContext, Consumer<RootContext> rootContextFunction) {
        if (mIsFinished.get()) {
            Log.e(TAG, "Trying to execute and document is finished. Ignoring...");
            return;
        }

        // Queue up the work if we're still preparing the document
        if (rootContext == null || !mIsDisplayed.get()) {
            mRunnableQueue.add(rootContextFunction);
        } else {
            rootContextFunction.accept(rootContext);
        }
    }

    @Override
    public void onDocumentRender(@NonNull RootContext rootContext) {
        mRootContext = rootContext;
    }

    @Override
    public void onDocumentFinish() {
        mIsFinished.set(true);
        mRunnableQueue.clear();
        mRootContext = null;
        mDocumentState = null;
    }

    @Override
    public void onDocumentDisplayed(long documentDisplayedTime) {
        mIsDisplayed.set(true);
        ITelemetryProvider telemetryProvider = getTelemetryProvider();
        if (telemetryProvider != null && mRenderDocumentTimer != null) {
            telemetryProvider.stopTimer(mRenderDocumentTimer, TimeUnit.MILLISECONDS, documentDisplayedTime);
        }
        IMetricsRecorder metricsRecorder = getMetricsRecorder();
        if (metricsRecorder != null) {
            metricsRecorder.recordMilestone(MetricsMilestoneConstants.RENDER_END_MILESTONE);
        }

        mMainHandler.post(this::processQueuedRunnables);
    }

    @MainThread
    private void processQueuedRunnables() {
        if (mRootContext != null) {
            Consumer<RootContext> pendingRunnable;
            while ((pendingRunnable = mRunnableQueue.poll()) != null) {
                pendingRunnable.accept(mRootContext);
            }
        }
    }

    /**
     * @return the APL version of the underlying document.
     */
    public int getDocVersion() {
        return APLVersionCodes.getVersionCode(mContent.getAPLVersion());
    }

    /**
     * @return True if the APL system is initialized.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static synchronized boolean isInitialized(final ITelemetryProvider telemetryProvider) {
        final int metricId = telemetryProvider.createMetricId(APL_DOMAIN,
                ITelemetryProvider.LIBRARY_INITIALIZATION_FAILED, COUNTER);
        try {
            if (sLibraryFuture != null) {
                return sLibraryFuture.get(ConcurrencyUtils.SMALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } else {
                Log.e(TAG, "Checking whether APL isInitialized before calling initializeAPL");
            }
        } catch (final TimeoutException ex) {
            telemetryProvider.incrementCount(metricId);
            Log.wtf(TAG, String.format("Library failed to load with a %d timeout", ConcurrencyUtils.SMALL_TIMEOUT_SECONDS), ex);
        } catch (final ExecutionException | InterruptedException ex) {
            telemetryProvider.incrementCount(metricId);
            Log.e(TAG, "The shared libraries failed to initialize", ex);
        }
        return false;
    }

    private ITelemetryProvider getTelemetryProvider() {
        if (mRootContext != null) {
            return mRootContext.getOptions().getTelemetryProvider();
        }
        return null;
    }

    private IMetricsRecorder getMetricsRecorder() {
        if (mRootContext != null) {
            return mRootContext.getMetricsRecorder();
        }
        return null;
    }

    private static FluidityIncidentReporter createFluidityIncidentCounter(final APLOptions aplOptions, final Context context) {
        ITelemetryProvider telemetryProvider = aplOptions.getTelemetryProvider();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        double frameTimeMs = 1000.0f / display.getRefreshRate();
        APLProperties aplProperties = new APLProperties();
        for (Map.Entry<String, Object> entry : aplOptions.getConfigurationMap().entrySet()) {
            aplProperties.set(entry.getKey(), entry.getValue());
        }
        return new FluidityIncidentReporter((int) aplProperties.get(APLProperty.kFluidityIncidentUpsWindowSize),
                telemetryProvider,
                aplProperties.getDouble(APLProperty.kFluidityIncidentUpsThreshold),
                frameTimeMs,
                aplProperties.getDouble(APLProperty.kFluidityIncidentMinimumDurationMs),
                telemetryProvider.createMetricId(APL_DOMAIN, RootContext.METRIC_FLUIDITY_INCIDENT_COUNT, COUNTER));
    }

    /**
     * Interface for building an APLController instance.
     */
    public static class Builder {
        private String aplDocument;
        private RootConfig rootConfig;
        private APLOptions aplOptions;
        private APLLayout aplLayout;
        private long startTime = SystemClock.elapsedRealtime();
        private InflationErrorCallback errorCallback = exception -> Log.e(TAG, "Exception inflating document!", exception);
        private IContentCreator contentCreator = Content::create;
        private boolean disableAsyncInflate;
        private DocumentSession documentSession;

        /**
         * Set the apl document. See https://aplspec.aka.corp.amazon.com/release-1.7/html/apl_document.html
         * @param aplDocument   the apl document
         * @return              this builder
         */
        public Builder aplDocument(@NonNull String aplDocument) {
            this.aplDocument = aplDocument;
            return this;
        }

        /**
         * Sets the root config for this document.
         * @param rootConfig    the root config
         * @return              this builder
         */
        public Builder rootConfig(@NonNull RootConfig rootConfig) {
            this.rootConfig = rootConfig;
            return this;
        }

        /**
         * Sets the APLOptions for this document.
         * @param options   the options
         * @return          this builder
         */
        public Builder aplOptions(@NonNull APLOptions options) {
            this.aplOptions = options;
            return this;
        }

        /**
         * Sets the APLLayout for this document.
         * @param layout    the APLLayout
         * @return          this builder
         */
        public Builder aplLayout(@NonNull APLLayout layout) {
            this.aplLayout = layout;
            return this;
        }

        /**
         * Sets the start time (based on the elapsed boot time) for this document for performance measurements.
         * Defaults to {@link SystemClock#elapsedRealtime()}.
         * @param startTime the start time in milliseconds
         * @return          this builder
         */
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Sets the error callback for if this document failed to inflate.
         * @param errorCallback the error callback
         * @return              this builder
         */
        public Builder errorCallback(@NonNull InflationErrorCallback errorCallback) {
            this.errorCallback = errorCallback;
            return this;
        }

        /**
         * Sets a Content creator for mocking.
         * @param contentCreator    the content creator.
         * @return                  this builder
         */
        @VisibleForTesting
        Builder contentCreator(@NonNull IContentCreator contentCreator) {
            this.contentCreator = contentCreator;
            return this;
        }

        public Builder disableAsyncInflate(boolean disableAsyncInflate) {
            this.disableAsyncInflate = disableAsyncInflate;
            return this;
        }

        public Builder documentSession(DocumentSession documentSession) {
            this.documentSession = documentSession;
            return this;
        }

        /**
         * Starts the render process for this document.
         *
         * Note: the returned {@link IAPLController} may be interacted with immediately
         * though the document won't be displayed until {@link IDocumentLifecycleListener#onDocumentDisplayed()} is called.
         * Any commands sent to this instance will be queued until the document is ready at which point they
         * will be executed in order received. If a document fails to render, then the commands will be discarded.
         * @return  An {@link IAPLController} instance.
         */
        public IAPLController render() {
            if (documentSession == null) {
                documentSession = DocumentSession.create();
            }
            return APLController.renderDocument(
                    Objects.requireNonNull(aplDocument),
                    Objects.requireNonNull(aplOptions),
                    Objects.requireNonNull(rootConfig),
                    Objects.requireNonNull(aplLayout),
                    Objects.requireNonNull(contentCreator),
                    startTime,
                    Objects.requireNonNull(errorCallback),
                    disableAsyncInflate,
                    Objects.requireNonNull(documentSession));
        }
    }

    /**
     * @return a default builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
