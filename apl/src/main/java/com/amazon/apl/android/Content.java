/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.dependencies.IContentDataRetriever;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.MetricsMilestoneConstants;
import com.amazon.apl.android.metrics.MetricsSegmentConstants;
import com.amazon.apl.android.metrics.impl.MetricsRecorder;
import com.amazon.apl.android.metrics.impl.NoOpMetricsRecorder;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.ColorUtils;
import com.amazon.apl.android.utils.JNIUtils;
import com.amazon.apl.devtools.enums.DTNetworkRequestType;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.common.BoundObject;
import com.amazon.apl.enums.GradientType;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Access to APL Document Contents.  These methods are called from the APLInflater worker thread.
 */
@SuppressWarnings("WeakerAccess")
public final class Content extends BoundObject {

    private static final String TAG = "Content";

    public static final String METRIC_CONTENT_CREATE = TAG + ".create";
    private int tContentCreate;

    public static final String METRIC_CONTENT_ERROR = TAG + ".error";
    private int cContentError;
    private ICounter mContentErrorCounter;

    private boolean usePackageManager;

    public static final String METRIC_CONTENT_IMPORT_REQUESTS = TAG + ".imports";
    private int cContentImportRequests;
    private ICounter mContentImportRequestCounter;

    // The outstanding list of ImportRequests.
    private final Set<ImportRequest> mImportRequests;

    // The data parameters
    private final Set<String> mParameters;

    // The document background
    @Nullable
    private DocumentBackground mDocumentBackground;

    @NonNull
    private final ITelemetryProvider mTelemetryProvider;
    @NonNull
    private final Handler mMainHandler;

    @Nullable
    private final IDTNetworkRequestHandler mDTNetworkRequestHandler;

    private IPackageLoader mPackageLoader;
    private IContentDataRetriever mDataRetriever;
    private CallbackV2 mCallback;

    private boolean mParametersOptional;

    private IMetricsRecorder mMetricsRecorder;

    private Map<ImportRef, APLJSONData> mPackages = new ConcurrentHashMap<>();

    private Handler mExecutionHandler;

    private Decodable mCachedDocumentSettings;

    /**
     * This Exception is thrown when a Content object cannot be created.
     */
    public static final class ContentException extends Exception {

        public ContentException(String message, Exception e) {
            super(message, e);
        }

        public ContentException(String message) {
            super(message);
        }

    }

    @NonNull
    public IMetricsRecorder getMetricsRecorder() {
        return mMetricsRecorder;
    }

    /**
     * Private constructor use {@link Content#create(String)} or {@link Content#create(String, Callback)}.
     */
    private Content(@NonNull ITelemetryProvider telemetryProvider,
                    @Nullable IPackageLoader packageLoader,
                    @Nullable IContentDataRetriever dataRetriever,
                    long entryTime,
                    @Nullable IDTNetworkRequestHandler dtNetworkRequest,
                    IMetricsRecorder metricsRecorder,
                    boolean parametersOptional) {
        // private constructor
        mImportRequests = Collections.newSetFromMap(new ConcurrentHashMap<>());
        mParameters = Collections.newSetFromMap(new ConcurrentHashMap<>());
        mParametersOptional = parametersOptional;
        mTelemetryProvider = telemetryProvider;

        tContentCreate = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_CREATE, TIMER);
        cContentError = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_ERROR, COUNTER);
        cContentImportRequests = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_IMPORT_REQUESTS, COUNTER);

        long currentTime = SystemClock.elapsedRealtimeNanos();
        mTelemetryProvider.startTimer(tContentCreate, NANOSECONDS, currentTime - entryTime);

        metricsRecorder.recordMilestone(MetricsMilestoneConstants.CONTENT_CREATE_START_MILESTONE);
        mContentErrorCounter = metricsRecorder.createCounter(APL_DOMAIN + "." + METRIC_CONTENT_ERROR);
        mContentImportRequestCounter = metricsRecorder.createCounter(APL_DOMAIN + "." + METRIC_CONTENT_IMPORT_REQUESTS);

        mDataRetriever = dataRetriever;
        setPackageLoader(packageLoader);
        mMainHandler = new Handler(Looper.getMainLooper());
        mExecutionHandler = mMainHandler;
        mDTNetworkRequestHandler = dtNetworkRequest;
        mMetricsRecorder = metricsRecorder;
        usePackageManager = false;
    }

    private Content(@NonNull ITelemetryProvider telemetryProvider,
                    @Nullable IPackageLoader packageLoader,
                    @Nullable IContentDataRetriever dataRetriever,
                    long entryTime,
                    @Nullable IDTNetworkRequestHandler dtNetworkRequest,
                    IMetricsRecorder metricsRecorder,
                    boolean parametersOptional,
                    Handler executionHandler) {
        this(telemetryProvider, packageLoader, dataRetriever, entryTime, dtNetworkRequest, metricsRecorder, parametersOptional);
        mExecutionHandler = executionHandler;
    }

    /**
     * Callback interface supplied to {@link Content#create(String, Callback)}
     * for receiving notifications about needed packages, data, and the status of the document import.
     *
     */
    @Deprecated
    public static abstract class Callback {
        /**
         * Request for an APL document containing a package. Implementors should respond by
         * calling {@link #addPackage(ImportRequest, String)}.
         */
        public void onPackageRequest(Content content, ImportRequest request) {
        }


        /**
         * Request for an APL document containing a data payload. Implementors should respond by
         * calling {@link #addData(String, String)}.
         */
        public void onDataRequest(Content content, String dataId) {
        }


        /**
         * The document import is complete.
         */
        public void onComplete(Content content) {
        }


        /**
         * The document has failed to import.
         */
        public void onError(Content content) {
        }

    }

    /**
     * Callback for creating Content. This uses the {@link IPackageLoader} and {@link IContentRetriever} supplied
     * in APL options.
     */
    public static abstract class CallbackV2 {
        /**
         * The document import is complete.
         */
        public void onComplete(Content content) {
        }


        /**
         * The document has failed to import.
         */
        public void onError(Exception e) {
        }

        /**
         * Any package was loaded and processed.
         */
        public void onPackageLoaded(Content content) {
        }
    }


    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     * Also registers a callback to receive Content ImportRequests and data parameters.
     *
     * @deprecated use {@link #create(String, APLOptions, CallbackV2, Session, IDTNetworkRequestHandler)} instead to use the package loader from APLOptions
     *
     * @param mainTemplate The APL document containing the 'mainTemplate' tag.
     * @param callback     A callback for Package and Data requests.
     * @return A Content object based on the mainTemplate document.
     */
    @NonNull
    @Deprecated
    public static Content create(@NonNull final String mainTemplate, final Callback callback) throws ContentException {
        long entryTime = SystemClock.elapsedRealtimeNanos(); // Must remain first for accurate telemetry!
        return createContent(mainTemplate, null, callback, null, false, entryTime, null, new Session(), null);
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     * Also registers a callback to receive Content ImportRequests and data parameters.
     *
     * @deprecated use {@link #create(String, APLOptions, CallbackV2, Session, IDTNetworkRequestHandler)} instead to use the package loader from APLOptions
     *
     * @param mainTemplate The APL document containing the 'mainTemplate' tag.
     * @param aplOptions   The APL options.
     * @param callback     A callback for Package and Data requests.
     * @return A Content object based on the mainTemplate document.
     */
    @Deprecated
    public static Content create(@NonNull final String mainTemplate, @NonNull final APLOptions aplOptions, final Callback callback) throws ContentException {
        long entryTime = SystemClock.elapsedRealtimeNanos(); // Must remain first for accurate telemetry!
        Objects.requireNonNull(aplOptions);
        return createContent(mainTemplate, aplOptions, callback, null, false, entryTime, null, new Session(), null);
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     *
     * @deprecated use {@link #create(String, APLOptions, CallbackV2, Session, IDTNetworkRequestHandler)} instead to use the package loader from APLOptions
     *
     * @param mainTemplate The main document.
     * @return A Content object based on the mainTemplate document.
     */
    @Deprecated
    @NonNull
    public static Content create(final String mainTemplate) throws ContentException {
        long entryTime = SystemClock.elapsedRealtimeNanos(); // Must remain first for accurate telemetry!
        return createContent(mainTemplate, null, null, null, false, entryTime, null, new Session(), null);
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     *
     * @deprecated use {@link #create(String, APLOptions, CallbackV2, Session, IDTNetworkRequestHandler)} instead to use the package loader from APLOptions
     *
     * @param mainTemplate The main document.
     * @param aplOptions   The APL options.
     * @return A Content object based on the mainTemplate document.
     */
    @Deprecated
    @NonNull
    public static Content create(final String mainTemplate, @NonNull final APLOptions aplOptions) throws ContentException {
        long entryTime = SystemClock.elapsedRealtimeNanos(); // Must remain first for accurate telemetry!
        Objects.requireNonNull(aplOptions);
        return createContent(mainTemplate, aplOptions, null, null, false, entryTime, null, new Session(), null);
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     *
     * @param mainTemplate      The main document.
     * @param aplOptions        The APL Options
     * @param callback          The callback for handling Content requests.
     * @param session           Experience logging session.
     * @param dtNetworkRequest  The IDTNetworkRequestHandler for notifying Dev Tools Clients of network events.
     * @return                  A Content object if the maintemplate is valid, otherwise null.
     */
    @Nullable
    public static Content create(final String mainTemplate, @NonNull final APLOptions aplOptions, @NonNull final CallbackV2 callback, Session session, IDTNetworkRequestHandler dtNetworkRequest) {
        long entryTime = SystemClock.elapsedRealtimeNanos();
        try {
            return createContent(mainTemplate, aplOptions, null, callback, false, entryTime, null, session, dtNetworkRequest);
        } catch (ContentException e) {
            callback.onError(e);
            return null;
        }
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     *
     * @param mainTemplate      The main document.
     * @param aplOptions        The APL Options
     * @param callback          The callback for handling Content requests.
     * @param session           Experience logging session.
     * @param dtNetworkRequest  The IDTNetworkRequestHandler for notifying Dev Tools Clients of network events.
     * @param parametersOptional True if the Content is allowed to complete without all parameters added.
     * @return                  A Content object if the maintemplate is valid, otherwise null.
     */
    @Nullable
    public static Content create(final String mainTemplate, @NonNull final APLOptions aplOptions, @NonNull final CallbackV2 callback, Session session, IDTNetworkRequestHandler dtNetworkRequest, boolean parametersOptional, Handler executionHandler) {
        long entryTime = SystemClock.elapsedRealtimeNanos();
        try {
            return createContent(mainTemplate, aplOptions, null, callback, parametersOptional, entryTime, null, session, dtNetworkRequest, executionHandler);
        } catch (ContentException e) {
            callback.onError(e);
            return null;
        }
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     *
     * @param mainTemplate     The main document.
     * @param aplOptions       The APL Options
     * @param callback         The callback for handling Content requests.
     * @param rootConfig       RootConfig
     * @param dtNetworkRequest The IDTNetworkRequestHandler for notifying Dev Tools Clients of network events.
     * @return A Content object if the maintemplate is valid, otherwise null.
     */
    @Nullable
    public static Content create(final String mainTemplate, @NonNull final APLOptions aplOptions, @NonNull final CallbackV2 callback, RootConfig rootConfig, IDTNetworkRequestHandler dtNetworkRequest) {
        long entryTime = SystemClock.elapsedRealtimeNanos();
        try {
            return createContent(mainTemplate, aplOptions, null, callback, false, entryTime, rootConfig,(rootConfig != null && rootConfig.getSession() != null) ? rootConfig.getSession() : new Session(), dtNetworkRequest);
        } catch (ContentException e) {
            callback.onError(e);
            return null;
        }
    }

    /**
     * Construct the working Content object from a document that contains the apl 'mainTemplate'.
     *
     * @param mainTemplate     The main document.
     * @param aplOptions       The APL Options
     * @param callback         The callback for handling Content requests.
     * @param rootConfig       RootConfig
     * @param dtNetworkRequest The IDTNetworkRequestHandler for notifying Dev Tools Clients of network events.
     * @param parametersOptional True if the Content is allowed to complete without all parameters added.
     * @return A Content object if the maintemplate is valid, otherwise null.
     */
    @Nullable
    public static Content create(final String mainTemplate, @NonNull final APLOptions aplOptions, @NonNull final CallbackV2 callback, RootConfig rootConfig, IDTNetworkRequestHandler dtNetworkRequest, boolean parametersOptional) {
        long entryTime = SystemClock.elapsedRealtimeNanos();
        try {
            return createContent(mainTemplate, aplOptions, null, callback, parametersOptional, entryTime, rootConfig,(rootConfig != null && rootConfig.getSession() != null) ? rootConfig.getSession() : new Session(), dtNetworkRequest);
        } catch (ContentException e) {
            callback.onError(e);
            return null;
        }
    }


    private static Content createContent(@NonNull final String mainTemplate,
                                         @Nullable final APLOptions aplOptions,
                                         @Nullable final Callback callback,
                                         @Nullable final CallbackV2 callbackV2,
                                         boolean parametersOptional,
                                         long entryTime,
                                         final RootConfig rootConfig,
                                         final Session session,
                                         final IDTNetworkRequestHandler dtNetworkRequestHandler) throws ContentException {
        if (mainTemplate.length() == 0) {
            throw new ContentException("Invalid document length.");
        }
        Content content = null;
        try {
            // TODO: Remove the NoOpMetricsRecorder when completely moving to the MetricsRecorder
            IMetricsRecorder metricsRecorder = BuildConfig.DEBUG ? new MetricsRecorder() : new NoOpMetricsRecorder();
            if (aplOptions != null && aplOptions.getMetricsOptions() != null) {
                List<IMetricsSink> metricsSinkList = aplOptions.getMetricsOptions().getMetricsSinkList() == null ?
                        Collections.emptyList() : aplOptions.getMetricsOptions().getMetricsSinkList();
                for (IMetricsSink sink: metricsSinkList) {
                    metricsRecorder.addSink(sink);
                }
                metricsRecorder.mergeMetadata(aplOptions.getMetricsOptions().getMetaData());
            }
            content = new Content(getTelemetryProvider(aplOptions), getPackageLoader(aplOptions), getDataRetriever(aplOptions), entryTime, dtNetworkRequestHandler, metricsRecorder, parametersOptional);
            content.setCallbacks(callbackV2, callback);
            content.importDocument(mainTemplate, rootConfig, session);
        } catch (Exception e) {
            if (content != null) {
                content.recordErrorState();
            }
            throw new ContentException("Could not create APL content:", e);
        }
        if (!content.isBound()) {
            content.recordErrorState();
            throw new ContentException("Invalid document.");
        }
        return content;
    }

    private static Content createContent(@NonNull final String mainTemplate,
                                         @Nullable final APLOptions aplOptions,
                                         @Nullable final Callback callback,
                                         @Nullable final CallbackV2 callbackV2,
                                         boolean parametersOptional,
                                         long entryTime,
                                         final RootConfig rootConfig,
                                         final Session session,
                                         final IDTNetworkRequestHandler dtNetworkRequestHandler,
                                         final Handler executionHandler) throws ContentException {
        if (mainTemplate.length() == 0) {
            throw new ContentException("Invalid document length.");
        }
        Content content = null;
        try {
            // TODO: Remove the NoOpMetricsRecorder when completely moving to the MetricsRecorder
            IMetricsRecorder metricsRecorder = BuildConfig.DEBUG ? new MetricsRecorder() : new NoOpMetricsRecorder();
            if (aplOptions != null && aplOptions.getMetricsOptions() != null) {
                List<IMetricsSink> metricsSinkList = aplOptions.getMetricsOptions().getMetricsSinkList() == null ?
                        Collections.emptyList() : aplOptions.getMetricsOptions().getMetricsSinkList();
                for (IMetricsSink sink: metricsSinkList) {
                    metricsRecorder.addSink(sink);
                }
                metricsRecorder.mergeMetadata(aplOptions.getMetricsOptions().getMetaData());
            }
            content = new Content(getTelemetryProvider(aplOptions), getPackageLoader(aplOptions), getDataRetriever(aplOptions), entryTime, dtNetworkRequestHandler, metricsRecorder, parametersOptional, executionHandler);
            content.setCallbacks(callbackV2, callback);
            content.importDocument(mainTemplate, rootConfig, session);
        } catch (Exception e) {
            if (content != null) {
                content.recordErrorState();
            }
            throw new ContentException("Could not create APL content:", e);
        }
        if (!content.isBound()) {
            content.recordErrorState();
            throw new ContentException("Invalid document.");
        }
        return content;
    }

    private void setCallbacks(CallbackV2 callbackV2, Callback callback) {
        mCallback = callbackV2;
        // Override callbacks and loaders if old callback is set.
        if (callback != null) {
            setPackageLoader((importRequest, onSuccess, onFailure) -> callback.onPackageRequest(Content.this, importRequest));
            mDataRetriever = (source, onSuccess, onFailure) -> callback.onDataRequest(Content.this, source);
            mCallback = new CallbackV2() {
                @Override
                public void onComplete(Content content) {
                    callback.onComplete(content);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(Content.this);
                }
            };
        }
    }

    private void setPackageLoader(IPackageLoader loader) {
        mPackageLoader = loader;
    }

    /**
     * Retrieve a set of packages that have been requested for import.  This method only returns an
     * individual package a single time.  Once it has been called, the "requested" packages
     * are moved internally into a "pending" list of packages.
     * <p>
     * This method should not be called when a callback is registered, the results are undefined.
     *
     * @return The set of packages that should be loaded.
     */
    @NonNull
    public Set<ImportRequest> getRequestedPackages() {
        Set<ImportRequest> requests = new HashSet<>(mImportRequests);
        mImportRequests.clear();
        return requests;
    }


    /**
     * Retrieve a set of parameters that represent needed data to satisfy this document import.
     * <p>
     * This method should not be called when a callback is registered, the results are undefined.
     *
     * @return The set of packages that should be loaded.
     */
    @NonNull
    public Set<String> getParameters() {
        return new HashSet<>(mParameters);
    }

    /**
     * Import the APL document.
     *
     * @param mainTemplate Contents of an APL document that contains the 'mainTemplate'.
     */
    private void importDocument(String mainTemplate, RootConfig rootConfig, Session session) {

        if(rootConfig != null && rootConfig.getPackageManager() != null) {
            usePackageManager = true;
        }

        long nativeHandle = nCreate(mainTemplate, rootConfig!= null ? rootConfig.getNativeHandle() : 0, session.getNativeHandle());
        if (nativeHandle != 0) {
            bind(nativeHandle);
            nUpdate(nativeHandle);
            // update the callback with both package and data requests
            notifyCallback(true, true);
        }
    }

    public boolean shouldUsePackageManager() {
        return usePackageManager;
    }

    /**
     * Add a package data to the content for a @{@link ImportRequest}. Will ignore the
     * package data if package with same name and version was already added.
     *
     * @param importRequest   The import request this contents satisfies.
     * @param packageContents The Contents of the package.
     * @throws ContentException if the package is empty.
     */
    synchronized public void addPackage(@NonNull ImportRequest importRequest,
                                        @NonNull String packageContents) throws ContentException {
        if (packageContents.length() == 0) {
            recordErrorState();
            throw new ContentException("Could not add APL package. Name: " + importRequest.getPackageName() + ", version: " + importRequest.getVersion());
        }

        addPackage(importRequest, APLJSONData.create(packageContents));
    }

    /**
     * Add a package @{@link APLJSONData} to the content for a @{@link ImportRequest}. Will ignore
     * the package data if package with same name and version was already added.
     *
     * @param importRequest The import request this contents satisfies.
     * @param jsonData The Contents of the package.
     */
    synchronized public void addPackage(@NonNull ImportRequest importRequest,
                                        @NonNull APLJSONData jsonData) {
        if (mPackages.containsKey(importRequest.getImportRef())) {
            Log.i(TAG, String.format("Package %s:%s was already added to content, ignoring request to re-add it",
                    importRequest.getPackageName(),
                    importRequest.getVersion()));
        } else {
            mPackages.put(importRequest.getImportRef(), jsonData);
        }
        nAddPackage(getNativeHandle(), importRequest.getNativeHandle(), jsonData.getNativeHandle());
        // update the callback with package requests, no need for data requests
        notifyCallback(true, true);
    }

    /**
     * Add data payload.
     *
     * @param dataId      The data identifier.
     * @param dataPayload The data payload.
     */
    synchronized public void addData(String dataId, String dataPayload) {
        nAddData(getNativeHandle(), dataId, dataPayload);
    }


    /**
     * Notifies the callback of new ImportRequests.
     *
     * @param notifyPackages Notify the callback of pending package requests.
     * @param notifyData     Notify the callback of pending data requests.
     */
    private void notifyCallback(@SuppressWarnings("SameParameterValue") boolean notifyPackages, boolean notifyData) {
        // Slightly awkward api where we don't do anything if a callback is not supplied.
        // All import/data requests are satisfied via manual calls from the runtime.
        if (mCallback == null) return;

        mCallback.onPackageLoaded(this);

        if (notifyPackages) {
            for (ImportRequest importRequest : getRequestedPackages()) {
                final long startTime = SystemClock.elapsedRealtimeNanos();
                Map<String, String> metaData = new HashMap<>();
                metaData.put("packageName", importRequest.getPackageName());
                ITimer packageImportTimer = mMetricsRecorder != null ? mMetricsRecorder.startTimer(MetricsSegmentConstants.CONTENT_PACKAGE_IMPORT_TIMER, metaData) : null;

                if (mDTNetworkRequestHandler != null) {
                    String source = importRequest.getSource();
                    if (TextUtils.isEmpty(source)) {
                        source = IDTNetworkRequestHandler.getDefaultPackageUrl(importRequest.getPackageName(), importRequest.getVersion());
                    }
                    if (IDTNetworkRequestHandler.isUrlRequest(source)) {
                        mDTNetworkRequestHandler.requestWillBeSent(importRequest.getRequestId(), SystemClock.elapsedRealtimeNanos(), source, DTNetworkRequestType.PACKAGE);
                    }
                }
                mPackageLoader.fetch(importRequest,
                        (ImportRequest request, APLJSONData result) ->  {
                            if (packageImportTimer != null) {
                                packageImportTimer.stop();
                            }
                            this.handleImportSuccess(request, result, startTime);
                        },
                        (ImportRequest request, String message) -> {
                            final long duration = NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startTime);
                            if (packageImportTimer != null) {
                                packageImportTimer.fail();
                            }
                            Log.e(TAG, String.format("Unable to load content for request: %s. Failed in %d milliseconds. %s",
                                    request,
                                    duration,
                                    message));
                            final String source = request.getSource();
                            if (mDTNetworkRequestHandler != null && IDTNetworkRequestHandler.isUrlRequest(source)) {
                                mDTNetworkRequestHandler.loadingFailed(request.getRequestId(), SystemClock.elapsedRealtimeNanos());
                            }
                        });
            }
        }
        if (notifyData) {
            for (String parameter : mParameters) {
                final long startTime = SystemClock.elapsedRealtimeNanos();
                Map<String, String> metaData = new HashMap<>();
                metaData.put("paramName", parameter);
                ITimer parameterLoadTimer = mMetricsRecorder != null ? mMetricsRecorder.startTimer(MetricsSegmentConstants.CONTENT_PARAMETER_LOAD_TIMER, metaData) : null;

                mDataRetriever.fetch(parameter,
                        (String param, String result) -> {
                            final long duration = NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startTime);
                            Log.i(TAG, String.format("Param '%s' took %d milliseconds to fetch.",
                                    param,
                                    duration));
                            if (parameterLoadTimer != null ) {
                                parameterLoadTimer.stop();
                            }
                            this.handleDataSuccess(param, result);
                        },
                        (String param, String message) -> {
                            final long duration =  NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startTime);
                            Log.e(TAG, String.format("Unable to fetch parameter: %s. Failed in %d milliseconds. %s",
                                    param,
                                    duration,
                                    message));
                            if (parameterLoadTimer != null ) {
                                parameterLoadTimer.fail();
                            }
                        });
            }
            //Parameters are now optional, so we need to complete if we're not waiting for any packages.
            if (mParametersOptional && !isReady() && !isError() && !isWaiting()) {
                Log.i(TAG, "Completing Content early due to optional parameters.");
                coreComplete();
            }
            mParameters.clear();
        }
    }

    private void handleImportSuccess(ImportRequest request, APLJSONData result, final long startTime) {
        final long duration = NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startTime);
        Log.i(TAG, String.format("Package '%s' took %d milliseconds to download.",
                request.getPackageName(),
                duration));

        final String source = request.getSource();
        if (mDTNetworkRequestHandler != null && IDTNetworkRequestHandler.isUrlRequest(source)) {
            mDTNetworkRequestHandler.loadingFinished(request.getRequestId(), SystemClock.elapsedRealtimeNanos(), result.getSize());
        }

       invokeOnMyThread(() -> addPackage(request, result));
    }

    private void handleDataSuccess(String param, String result) {
        invokeOnMyThread(() -> addData(param, result));
    }

    private void invokeOnMyThread(Runnable runnable) {
        if (Thread.currentThread() == mExecutionHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            mExecutionHandler.post(runnable);
        }
    }

    /**
     * Respond to an APL Core request for a needed package.
     *
     * @param nativeHandle Handle to the native peer ImportRequest.
     * @param name         The package name.
     * @param version      The package version.
     * @param domain       The package domain.
     */
    @SuppressWarnings("unused")
    private void coreRequestPackage(long nativeHandle, String source, String name, String version, String domain) {
        mTelemetryProvider.incrementCount(cContentImportRequests);
        if (mMetricsRecorder != null) {
            mContentImportRequestCounter.increment(1);
        }
        mImportRequests.add(new ImportRequest(nativeHandle, source, name, version, domain, IDTNetworkRequestHandler.IdGenerator.generateId()));
    }

    /**
     * Respond to an APL Core request for a needed package.
     */
    @SuppressWarnings("unused")
    private void coreRequestData(String dataId) {
        mParameters.add(dataId);
    }

    /**
     * Respond to an APL Core document complete.
     */
    @SuppressWarnings("unused")
    private void coreComplete() {
        recordSuccessState();
        if (mCallback != null) {
            mCallback.onComplete(this);
            mCallback = null;
        }
    }


    /**
     * Respond to an APL Core document failure.
     */
    @SuppressWarnings("unused")
    private void coreFailure() {
        recordErrorState();
        if (mCallback != null) {
            mCallback.onError(new ContentException("Content Error."));
            mCallback = null;
        }
    }

    /**
     * @return true if this document is waiting for a number of packages to be loaded.
     */
    public boolean isWaiting() {
        return nIsWaiting(getNativeHandle());
    }

    public void refresh(long embeddedRequestHandle, final long documentConfigHandle) {
        nRefresh(getNativeHandle(), embeddedRequestHandle, documentConfigHandle);
    }

    /**
     * @return true if this content is complete and ready to be inflated.
     */
    public boolean isReady() {
        return nIsReady(getNativeHandle());
    }

    /**
     * @return true if this content is in an error state and can't be inflated.
     */
    public boolean isError() {
        return nIsError(getNativeHandle());
    }

    private void recordSuccessState() {
        mTelemetryProvider.stopTimer(tContentCreate);
        if (mMetricsRecorder != null) {
            mMetricsRecorder.recordMilestone(MetricsMilestoneConstants.CONTENT_CREATE_END_MILESTONE);
        }
    }

    private void recordErrorState() {
        mTelemetryProvider.fail(tContentCreate);
        mTelemetryProvider.incrementCount(cContentError);
        if (mMetricsRecorder != null) {
            mMetricsRecorder.recordMilestone(MetricsMilestoneConstants.CONTENT_CREATE_FAILED_MILESTONE);
            mContentErrorCounter.increment(1);
        }
    }


    /**
     * @return The document version.
     */
    @NonNull
    public String getAPLVersion() {
        return nGetAPLVersion(getNativeHandle());
    }

    /**
     * Defines a package needed to satisfy document inflation.
     */
    public static class ImportRequest extends BoundObject {
        /**
         * Use {@link #getSource()}
         **/
        @Deprecated
        final public String source;
        /**
         * Use {@link #getPackageName()}
         **/
        @Deprecated
        final public String packageName;
        /** Use @link{#getVersion} **/
        @Deprecated
        final public String version;

        private final ImportRef mImportRef;
        private final int mRequestId;

        ImportRequest(long nativeHandle, String source, String packageName, String version, String domain, int requestId) {
            bind(nativeHandle);
            // TODO replace these fields with native call to bound object
            this.source = source;
            this.packageName = packageName;
            this.version = version;
            mImportRef = ImportRef.create(packageName, version, domain);
            mRequestId = requestId;
        }


        @NonNull
        public String getPackageName() {
            return packageName;
        }

        @NonNull
        public String getVersion() {
            return version;
        }

        @Nullable
        public String getSource() {
            return source;
        }

        public ImportRef getImportRef() {
            return mImportRef;
        }

        public int getRequestId() {
            return mRequestId;
        }
    }

    /**
     * Unique identifier of an Import.
     */
    @AutoValue
    public static abstract class ImportRef {
        public abstract String name();
        public abstract String version();

        public abstract String domain();

        public static ImportRef create(String name, String version, String domain) {
            return new AutoValue_Content_ImportRef(name, version, domain);
        }

        public static ImportRef create(String name, String version) {
            return new AutoValue_Content_ImportRef(name, version, "");
        }
    }


    /**
     * Utility method to log file contents that exceeds logcat message length.
     *
     * @param content content of file to be logged
     */
    @SuppressWarnings({"unused", "ConstantConditions"})
    public static void logFileContent(@NonNull String tag, @NonNull String content) {

        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = content.length(); i < length; i++) {
            int newline = content.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + 1000);
                Log.v(tag, content.substring(i, end));
                i = end;
            } while (i < newline);
        }
    }

    /**
     * @return The set of requested extensions (a collection of URI values)
     */
    @NonNull
    public Set<String> getExtensionRequests() {
        return nGetExtensionRequests(getNativeHandle());
    }

    /**
     * Retrieve the settings associated with an extension request.
     *
     * @param uri The uri of the extension.
     * @return Extension settings Map, null if no settings are specified in the document.
     */
    @Nullable
    public Map<String, Object> getExtensionSettings(@NonNull String uri) {
        return nGetExtensionSettings(getNativeHandle(), uri);
    }


    public static class DocumentBackground {
        private final boolean mIsGradient;
        private final int mColor;
        private final GradientType mType;
        private final float mAngle;
        private final @Nullable int[] mColorRange;
        private final @Nullable float[] mInputRange;

        DocumentBackground(boolean isGradient, int type, long color, float angle,
                           @Nullable long[] colorRange, @Nullable float[] inputRange) {
            mType = GradientType.valueOf(type);
            mAngle = angle;
            mInputRange = inputRange;
            mColorRange = colorRange != null ? Gradient.convertColorRange(colorRange) : null;
            mColor = ColorUtils.toARGB(color);
            mIsGradient = isGradient;
        }

        public boolean isColor() {
            return !mIsGradient;
        }

        public int getColor() {
            return mColor;
        }

        public boolean isGradient() {
            return mIsGradient;
        }

        public GradientType getType() {
            return mType;
        }

        public float getAngle() {
            return mAngle;
        }

        public int[] getColorRange() {
            return mColorRange;
        }

        public float[] getInputRange() {
            return mInputRange;
        }
    }

    /**
     * Retrieve the document background object with metrics and root config.
     * @param metrics
     * @param rootConfig
     * @callback callbackBackgroundGradient or callbackBackgroundColor
     */
    @Nullable
    public void createDocumentBackground(@NonNull ViewportMetrics metrics, @NonNull RootConfig rootConfig) {
        nCreateDocumentBackground(getNativeHandle(), rootConfig.getNativeHandle(),
                                metrics.width(), metrics.height(), metrics.dpi(),
                                metrics.shape().getIndex(), metrics.theme(), metrics.mode().getIndex());
    }


    /**
     * Callback for JNI to create document gradient background.
     *
     * @param type gradient type
     * @param angle gradient angle
     * @param colorRange gradient color range array
     * @param inputRange gradient input range array
     */
    @SuppressWarnings("unused")
    private void callbackBackgroundGradient(int type, float angle,long[] colorRange, float[] inputRange) {
        mDocumentBackground = new DocumentBackground(true, type, Color.TRANSPARENT, angle, colorRange, inputRange);
    }

    /**
     * Callback for JNI to create document color background.
     *
     * @param color
     */
    @SuppressWarnings("unused")
    private void callbackBackgroundColor(long color) {
        mDocumentBackground = new DocumentBackground(false, 0, color, 0, null, null);
    }

    public DocumentBackground getDocumentBackground() {
        return mDocumentBackground;
    }

    private static @NonNull
    ITelemetryProvider getTelemetryProvider(@Nullable APLOptions aplOptions) {
        if (aplOptions != null) {
            return aplOptions.getTelemetryProvider();
        } else {
            return NoOpTelemetryProvider.getInstance();
        }
    }

    private static IPackageLoader getPackageLoader(@Nullable APLOptions options) {
        if (options != null) {
            return options.getPackageLoader();
        }

        return ((importRequest, successCallback, failureCallback) -> failureCallback.onFailure(importRequest, "Not implemented."));
    }

    private static IContentDataRetriever getDataRetriever(@Nullable APLOptions options) {
        if (options != null) {
            return options.getContentDataRetriever();
        }

        return ((request, successCallback, failureCallback) -> failureCallback.onFailure(request, "Not implemented."));
    }

    /**
     * Returns whether or not the setting was set in the document.
     *
     * @param propertyName the name of the setting.
     * @return true if the setting was set in the document,
     * false otherwise.
     */
    public boolean hasSetting(String propertyName) {
        return nSetting(getNativeHandle(), propertyName) != null;
    }

    /**
     * Gets the setting value stored in the APL Document or the fallback value if it wasn't set.
     *
     * @param propertyName the value in the apl document
     * @param defaultValue the fallback value if it is not set
     * @param <K>          the type of the expected setting value
     * @return the setting value
     */
    @SuppressWarnings("unchecked")
    public <K> K optSetting(String propertyName, K defaultValue) {
        Object value = nSetting(getNativeHandle(), propertyName);
        // If the value isn't found or doesn't match the default value's class, then fallback
        if (value == null || (defaultValue != null && value.getClass() != defaultValue.getClass())) {
            return defaultValue;
        }
        return (K) value;
    }

    /**
     * @return  Returns the setting value stored in the APL Document
     */
    public Decodable getSerializedDocumentSettings(@NonNull RootConfig rootConfig) throws JSONException {
        if (mCachedDocumentSettings != null) {
            return mCachedDocumentSettings;
        }
        mCachedDocumentSettings = new JsonDecodable(new JSONObject(JNIUtils.safeStringValues(nGetSerializedDocumentSettings(getNativeHandle(), rootConfig.getNativeHandle()))));
        return mCachedDocumentSettings;
    }

    /**
     * resolve request after conditional imports.
     * @param callback
     */
    public void resolve(CallbackV2 callback, RootConfig configOfParent) {

        // Update usePackageManager according to availability of package manager
        // in parent and hence host component.
        if(configOfParent != null)
            usePackageManager = configOfParent.getPackageManager() != null;

        mCallback = callback;
        nUpdate(getNativeHandle());
        notifyCallback(true, true);
    }

    /**
     * JNI call to core, document import. This creates the native peer and
     * initializes the document.  OnFailure may be called if the document is invalid.
     */
    private native long nCreate(String mainTemplate, long _rootConfigHandler, long _sessionHandle);

    private native void nUpdate(long nativeHandle);

    private native void nRefresh(long nativeHandle, long embeddedRequestHandle, long documentConfigHandle);

    private native void nAddPackage(long nativeHandle, long requestId, long aplJsonData);

    private native void nAddData(long nativeHandle, String dataId, String dataPayload);

    private static native boolean nIsWaiting(long nativeHandle);

    private static native boolean nIsReady(long nativeHandle);

    private static native boolean nIsError(long nativeHandle);

    private static native Object nSetting(long nativeHandle, String settingName);

    private static native String nGetSerializedDocumentSettings(long nativeHandle, long _rootConfigHandler);

    @NonNull
    private static native Set<String> nGetExtensionRequests(long nativeHandle);

    @Nullable
    private static native Map<String, Object> nGetExtensionSettings(long nativeHandle, String uri);

    @NonNull
    private static native String nGetAPLVersion(long nativeHandle);

    private native void nCreateDocumentBackground(long nativeHandle, long rootConfigHandle,
                                               //metrics parameters
                                               int width, int height, int dpi,
                                               int screenShape, String theme,
                                               int viewportMode);
}

