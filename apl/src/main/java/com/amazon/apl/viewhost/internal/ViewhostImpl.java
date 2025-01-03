/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.ExtensionMediator.ILoadExtensionCallback;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.PackageManager;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Session;
import com.amazon.apl.android.UserPerceivedFatalReporter;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.dependencies.impl.NoOpUserPerceivedFatalCallback;
import com.amazon.apl.android.events.DataSourceFetchEvent;
import com.amazon.apl.android.events.OpenURLEvent;
import com.amazon.apl.android.events.RefreshEvent;
import com.amazon.apl.android.events.SendEvent;
import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.android.thread.Threading;
import com.amazon.apl.devtools.DevToolsProvider;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.internal.message.action.ActionMessageImpl;
import com.amazon.apl.viewhost.internal.message.action.FetchDataRequestImpl;
import com.amazon.apl.viewhost.internal.message.action.ReportRuntimeErrorRequestImpl;
import com.amazon.apl.viewhost.internal.message.action.SendUserEventRequestImpl;
import com.amazon.apl.viewhost.internal.message.notification.NotificationMessageImpl;
import com.amazon.apl.viewhost.message.MessageHandler;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateViewStateRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Internal implementation of the viewhost
 */
public class ViewhostImpl extends Viewhost implements DocumentStateChangeListener {
    private static final String TAG = "ViewhostImpl";
    private static final String FETCH_DATA_REQUEST = "FetchDataRequest";
    private static final String SEND_USER_EVENT_REQUEST = "SendUserEventRequest";
    private static final String OPEN_URL_REQUEST = "OpenURLRequest";
    private final ViewhostConfig mConfig;

    private final Executor mRuntimeInteractionWorker;
    private final Handler mCoreWorker;
    private final AtomicInteger mNextMessageId = new AtomicInteger(1);
    private static final String EMPTY = "";
    private final DTNetworkRequestManager mDTNetworkRequestManager = new DTNetworkRequestManager();

    private APLLayout mAplLayout;
    private DevToolsProvider mDevToolsProvider;

    private final Set<DocumentStateChangeListener> mDocumentStateChangeListeners = new HashSet<>();

    private ITelemetryProvider mTelemetryProvider = NoOpTelemetryProvider.getInstance();

    private static final String RENDER_DOCUMENT_COUNT_TAG = "Viewhost." + ITelemetryProvider.RENDER_DOCUMENT + "Count";
    private Integer mRenderDocumentCount;

    private final APLProperties mProperties = new APLProperties();

    /**
     * Maintain a map of documents that are known to the new viewhost. This is needed for event
     * routing.
     */
    private final Map<Long, WeakReference<DocumentHandleImpl>> mDocumentMap = new ConcurrentHashMap<>();

    @Nullable
    private RootContext mRootContext;

    @Nullable
    private DocumentHandle mTopDocument;

    // Display state of the view.
    //
    // Note: Fully-scoped to distinguish the VH:core and runtime:VH contract.
    //       - com.amazon.apl.enums.DisplayState is used for the VH:core contract
    //       - com.amazon.apl.viewhost.primitives.DisplayState is used for the runtime:VH contract
    private com.amazon.apl.viewhost.primitives.DisplayState mDisplayState =
            com.amazon.apl.viewhost.primitives.DisplayState.FOREGROUND;
    // -1 is unthrottled, 0 is paused, non-zero is the maximum frame rate in Hz
    private double mProcessingRate = -1.0;

    public ViewhostImpl(ViewhostConfig config, Executor runtimeInteractionWorker, Handler coreWorker) {
        mConfig = config;
        init();
        mRuntimeInteractionWorker = runtimeInteractionWorker;
        mCoreWorker = coreWorker;
    }

    public ViewhostImpl(ViewhostConfig config) {
        mConfig = config;
        init();
        mRuntimeInteractionWorker = Threading.createSequentialExecutor();
        Map<String, Object> properties = config.getDefaultDocumentOptions().getProperties();
        if (properties != null && properties.containsKey("inflateOnMainThread") && (boolean) properties.get("inflateOnMainThread")) {
            mCoreWorker = new Handler(Looper.getMainLooper());
        } else {
            HandlerThread thread = new HandlerThread("BackgroundHandler");
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
            mCoreWorker = new Handler(thread.getLooper());
        }
    }

    private void init() {
        if (mConfig.getDefaultDocumentOptions() != null && mConfig.getDefaultDocumentOptions().getTelemetryProvider() != null) {
            mTelemetryProvider = mConfig.getDefaultDocumentOptions().getTelemetryProvider();
        }
        mRenderDocumentCount = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, RENDER_DOCUMENT_COUNT_TAG, ITelemetryProvider.Type.COUNTER);
    }

    @Override
    public PreparedDocument prepare(final PrepareDocumentRequest request) {
        Log.i(TAG, "Prepare called with token: " + request.getToken());
        // For first iteration of M1 milestone we are keeping the content creation in this block. May move it later
        String document = request.getDocument() != null ? ((JsonStringDecodable) request.getDocument()).getString() : EMPTY;
        APLOptions options = createAPLOptions(request);
        // Merge the two document options into one.
        DocumentOptions documentOptions = mConfig.getDefaultDocumentOptions().merge(request.getDocumentOptions());

        DocumentHandleImpl handle = new DocumentHandleImpl(this, mCoreWorker, options.getMetricsOptions());
        handle.setRootConfig(createRootConfig());
        handle.setToken(request.getToken());
        if (documentOptions != null && documentOptions.getUserPerceivedFatalCallback() != null) {
            handle.setUserPerceivedFatalReporter(new UserPerceivedFatalReporter(documentOptions.getUserPerceivedFatalCallback()));
        }
        DocumentSession session = request.getDocumentSession();
        session.setCoreWorker(mCoreWorker);

        if (documentOptions != null && documentOptions.getTelemetryProvider() != null) {
            handle.setTelemetryProvider(documentOptions.getTelemetryProvider());
        }

        Content.create(document, options, new Content.CallbackV2() {

            @Override
            public void onComplete(Content content) {
                if (documentOptions != null) {
                    handle.setDocumentOptions(documentOptions);
                }

                if (mConfig.getLegacyExtensionRegistration() != null && handle.getRootConfig() != null) {
                    mConfig.getLegacyExtensionRegistration().registerExtensions(content, handle.getRootConfig());
                }

                // Fetch the registrar in the following order of preference:
                //      1. DocumentOptions
                //      2. Viewhost Config
                final ExtensionRegistrar registrar = (documentOptions != null) ? documentOptions.getExtensionRegistrar() : mConfig.getExtensionRegistrar();

                final ExtensionMediator mediator = (registrar != null) ? ExtensionMediator.create(registrar, session) : null;
                if (mediator != null) {
                    handle.setExtensionMediator(mediator);
                }

                if (mediator != null && documentOptions != null && documentOptions.getExtensionGrantRequestCallback() != null) {
                    loadExtensionAndSetContent(mediator, documentOptions, content, handle, false);
                } else {
                    handle.setContent(content);
                }

                session.bind(handle);
                session.onSessionEnded(sessionEndedCallback -> {
                    mCoreWorker.post(() -> {
                        if (mediator != null) {
                            mediator.onSessionEnded();
                        }
                    });
                });
            }

            @Override
            public void onError(Exception e) {
                Log.i(TAG, e.toString());
                handle.getUserPerceivedFatalReporter().reportFatal(UserPerceivedFatalReporter.UpfReason.CONTENT_CREATION_FAILURE);
                handle.setDocumentState(DocumentState.ERROR);
                writeAPLSessionLog(handle.getSession(), Session.LogEntryLevel.ERROR, "Document Failed: " + e.getMessage());
            }

            @Override
            public void onPackageLoaded(Content content) {
                // do nothing when packages are loaded
            }
        }, handle.getSession(), mDTNetworkRequestManager, true, mCoreWorker);

        return new PreparedDocumentImpl(handle);
    }

    /**
     * Loads extension and set content.
     *
     * @param mediator
     * @param documentOptions
     * @param content
     * @param handle
     * @param isRefresh
     */
    private void loadExtensionAndSetContent(@NonNull ExtensionMediator mediator, @NonNull DocumentOptions documentOptions,
                                            Content content, @NonNull DocumentHandleImpl handle, boolean isRefresh) {
        Map<String, Object> flags = documentOptions.getExtensionFlags() != null ? documentOptions.getExtensionFlags() : new HashMap<>();
        mediator.initializeExtensions(flags, content, documentOptions.getExtensionGrantRequestCallback());

        final int extensionRegistrationMetric = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, ITelemetryProvider.METRIC_TIMER_EXTENSION_REGISTRATION, ITelemetryProvider.Type.TIMER);
        mTelemetryProvider.startTimer(extensionRegistrationMetric);
        mediator.loadExtensions(
                flags,
                content,
                new ILoadExtensionCallback() {
                    @Override
                    public Runnable onSuccess() {
                        mTelemetryProvider.stopTimer(extensionRegistrationMetric);
                        if (isRefresh) {
                            return () -> Log.i(TAG, "Content refresh successful");
                        }
                        return () -> handle.setContent(content);
                    }

                    @Override
                    public Runnable onFailure() {
                        mTelemetryProvider.fail(extensionRegistrationMetric);
                        handle.getUserPerceivedFatalReporter().reportFatal(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE);
                        return () -> handle.setDocumentState(DocumentState.ERROR);
                    }
                }
        );
    }

    private APLOptions createAPLOptions(final PrepareDocumentRequest request) {
        // Since Data is optional, if it is not included, this will be serialized into a null content in APLOptions,
        // and ignored later in the content creation process
        String data = request.getData() != null ? ((JsonStringDecodable) request.getData()).getString() : EMPTY;
        // In order to re-use the existing Content::Create methods creating an APLOptions instance here
        // This wil be eventually phased out in favor of other unified Viewhost APIs
        IPackageLoader packageLoader = mConfig.getIPackageLoader() == null ? (importRequest, successCallback, failureCallback) -> failureCallback.onFailure(importRequest, "Content package loading not implemented.") : mConfig.getIPackageLoader();
        // Merge the two document options into one.
        DocumentOptions documentOptions = mConfig.getDefaultDocumentOptions().merge(request.getDocumentOptions());

        MetricsOptions metricsOptions;
        if (documentOptions != null && documentOptions.getMetricsOptions() != null) {
            metricsOptions = documentOptions.getMetricsOptions();
        } else {
            metricsOptions = MetricsOptions.builder().metricsSinkList(Collections.emptyList()).build();
        }
        APLOptions options = APLOptions.builder()
                .packageLoader(packageLoader)
                .telemetryProvider(mTelemetryProvider)
                .metricsOptions(metricsOptions)
                .contentDataRetriever((source, successCallback, failureCallback) -> {
                    JSONObject currentData;
                    try {
                        // attempts to create a JSONObject with the data to adhere with the existing implementation of contentretriever
                        // data is bundled into APLOptions here
                        currentData = new JSONObject(data);
                    } catch (JSONException e) {
                        // if data cannot be serialized into a JSONObject then use an empty object
                        currentData = new JSONObject();
                    }
                    if (currentData.has(source)) {
                        successCallback.onSuccess(source, currentData.optString(source));
                    } else if (source.equals("payload")) { // payload has special handling for backwards compatibility.
                        successCallback.onSuccess(source, currentData.toString());
                    }
                })
                .build();
        return options;
    }

    @Override
    public DocumentHandle render(final RenderDocumentRequest request) {
        Log.d(TAG, "Render called using RenderDocumentRequest with token: " + request.getToken());
        final long entryTime = SystemClock.elapsedRealtime();
        PreparedDocument preparedDocument = prepare(
                PrepareDocumentRequest.builder()
                        .token(request.getToken())
                        .document(request.getDocument())
                        .data(request.getData())
                        .documentSession(request.getDocumentSession())
                        .documentOptions(request.getDocumentOptions())
                        .build()
        );

        return render(preparedDocument, SystemClock.elapsedRealtime() - entryTime);
    }

    @Override
    public DocumentHandle render(final PreparedDocument preparedDocument) {
        Log.d(TAG, "Render called using PreparedDocument with token: " + preparedDocument.getToken());
        return render(preparedDocument, 0);
    }

    DocumentHandle render(final PreparedDocument preparedDocument, final long initialElapsedTime) {
        DocumentHandleImpl handle = preparedDocument.getHandle() instanceof DocumentHandleImpl ?
                (DocumentHandleImpl) preparedDocument.getHandle() : null;
        if (handle == null) {
            Log.e(TAG, "Document handle is unexpectedly null, dropping request to render document");
            return null;
        }
        if (!handle.isValid()) {
            Log.e(TAG, "Document not valid, dropping request to render document");
            return null;
        }
        writeAPLSessionLog(handle.getSession(), Session.LogEntryLevel.INFO, "----- Rendering Document -----");

        handle.startRenderDocumentTimer(TimeUnit.MILLISECONDS, initialElapsedTime);
        Log.i(TAG, "Rendering document with handle: " + handle);

        for (DocumentStateChangeListener listener : mDocumentStateChangeListeners) {
            handle.registerStateChangeListener(listener);
        }
        handle.registerStateChangeListener(this);
        return handle;
    }

    @Override
    public void registerStateChangeListener(DocumentStateChangeListener listener) {
        mDocumentStateChangeListeners.add(listener);
    }

    @Override
    public void bind(@NonNull APLLayout aplLayout) {
        if (mAplLayout != null) {
            throw new IllegalStateException("There is already a view bound");
        } else {
            mAplLayout = aplLayout;
            if (mAplLayout != null) {
                mDevToolsProvider = ((APLLayout.APLViewPresenterImpl)aplLayout.getPresenter()).getDevToolsProvider();
                mDTNetworkRequestManager.bindDTNetworkRequest(mDevToolsProvider.getNetworkRequestHandler());
            }

            DocumentHandleImpl topDocument = (DocumentHandleImpl) mTopDocument;
            if (mTopDocument != null && topDocument.getContent() != null &&
                    (DocumentState.DISPLAYED.equals(topDocument.getDocumentState()) || DocumentState.INFLATED.equals(topDocument.getDocumentState()))) {
                Log.i(TAG, "Reusing prepared document to render");
                inflate(topDocument);

            }
        }
    }

    @Override
    public void unBind() {
        mAplLayout = null;
        mDTNetworkRequestManager.unbindDTNetworkRequest();
    }

    @Override
    public boolean isBound() {
        return mAplLayout != null;
    }

    @Override
    public void updateDisplayState(DisplayState displayState) {
        Log.d(TAG, "Received updateDisplayState for handle: " + mTopDocument + " with display state: " + displayState);
        RootContext rootContext = mRootContext;
        if (rootContext != null) {
            rootContext.updateDisplayState(displayState);

            if (displayState == DisplayState.kDisplayStateBackground || displayState == DisplayState.kDisplayStateForeground) {
                rootContext.resumeDocument();
            } else { // If Hidden
                rootContext.pauseDocument();
            }
        } else {
            Log.w(TAG, "Skipping DisplayState update due to no root context.");
        }
    }

    @Override
    public void updateViewState(UpdateViewStateRequest request) {
        UpdateViewStateRequest.UpdateViewStateCallback callback = request.getCallback();
        mCoreWorker.post(() -> { 
            if (request.getDisplayState() != null) {
                mDisplayState = request.getDisplayState();
            }
            if (request.getProcessingRate() != null) {
                mProcessingRate = request.getProcessingRate();
            }

            applyViewState(mRootContext);
            if (callback != null) {
                publish(() -> callback.onComplete());
            }
        });
    }

    private void applyViewState(RootContext rootContext) {
        if (rootContext == null) {
            Log.d(TAG, "View state not applied, as there is no root context currently.");
            return;
        }

        Log.d(TAG, String.format("Applying view state displayState=%s, processingRate=%.2f",
                mDisplayState, mProcessingRate));
        mRootContext.updateDisplayState(getCoreDisplayState());
        mRootContext.setProcessingRate(mProcessingRate);
    }

    private DisplayState getCoreDisplayState() {
        switch (mDisplayState) {
            case HIDDEN:
                return DisplayState.kDisplayStateHidden;
            case BACKGROUND:
                return DisplayState.kDisplayStateBackground;
        }
        return DisplayState.kDisplayStateForeground;
    }

    public com.amazon.apl.viewhost.primitives.DisplayState getDisplayState() {
        return mDisplayState;
    }

    public double getProcessingRate() {
        return mProcessingRate;
    }

    @Override
    public void cancelExecution() {
        Log.d(TAG, "Received cancelExecution for handle: " + mTopDocument);
        if (mRootContext != null) {
            mCoreWorker.post(() -> mRootContext.cancelExecution());
        } else {
            Log.w(TAG, "RootContext is null, skipping cancelExecution");
        }
    }

    /**
     * TODO: Revisit the restore API design.
     * @param savedDocument The document to restore.
     *
     */
    @Override
    public boolean restoreDocument(SavedDocument savedDocument) {
        DocumentHandleImpl documentHandle = ((DocumentHandleImpl)savedDocument.getDocumentHandle());
        if (documentHandle == null) {
            Log.e(TAG, "Failed to restore saved document due to null document handle.");
            return false;
        }

        Log.i(TAG, "Trying to restore document: " + documentHandle);

        if (!documentHandle.isValid()) {
            Log.e(TAG, "Failed to restore saved document which is not valid.");
            return false;
        }

        if (mTopDocument != null && mTopDocument.equals(documentHandle) && mTopDocument.isValid()) {
            Log.w(TAG, "Trying to restore the valid top document hence no action needed");
            return false;
        }

        if (mTopDocument != null && mTopDocument.isValid()) {
            mTopDocument.finish(FinishDocumentRequest.builder().build());
        }

        documentHandle.setDocumentState(DocumentState.PREPARED);
        return true;
    }

    @Override
    public void invokeExtensionEventHandler(@NonNull String uri, @NonNull String name, Map<String, Object> data, boolean fastMode, @Nullable ExtensionEventHandlerCallback callback) {
        if (mRootContext != null) {
            mCoreWorker.post(() -> {
                Action action = mRootContext.invokeExtensionEventHandler(uri, name, data, fastMode);
                if (callback != null) {
                    if (action != null) {
                        // Pending action, add callbacks
                        action.then(() -> {
                            publish(() -> callback.onComplete());
                        });
                        action.addTerminateCallback(() -> {
                            publish(() -> callback.onTerminated());
                        });
                    } else {
                        // Not waiting for an action, execute the success callback immediately
                        publish(() -> {
                            callback.onComplete();
                        });
                    }
                }
            });
        } else {
            Log.w(TAG, "RootContext is null, skipping invokeExtensionEventHandler");
        }
    }

    /**
     * Internal method for invoking a Runnable on the runtime thread
     * @param task
     */
    public void publish(Runnable task) {
        mRuntimeInteractionWorker.execute(task);
    }

    /**
     * Internal method for notifying viewhost about document context assignments
     * @param handle The document handle
     */
    public synchronized void updateDocumentMap(DocumentHandle handle) {
        cleanDocumentMap();

        Long key = ((DocumentHandleImpl)handle).getDocumentContext().getId();
        mDocumentMap.put(key, new WeakReference<>(((DocumentHandleImpl)handle)));
    }

    public void notifyRootContextFinished() {
        for (WeakReference<DocumentHandleImpl> weakDocumentHandle : mDocumentMap.values()) {
            if (null == weakDocumentHandle) {
                continue;
            }
            final DocumentHandleImpl document = weakDocumentHandle.get();
            if (document != null) {
                document.setDocumentState(DocumentState.FINISHED);
            }
        }
    }

    /**
     * Shim for new viewhost abstraction to peek at legacy events and modify them as required to
     * route them correctly to the new abstractions.
     *
     * @param event The event which may be modified
     * @return true if event execution should proceed, or false if the event should be dropped
     */
    public boolean interceptEventIfNeeded(Event event) {
        Log.i(TAG, "intercepting event: " + event.getClass().getName());
        Long key = event.getDocumentContextId();
        WeakReference<DocumentHandleImpl> weakDocumentHandle = mDocumentMap.get(key);
        if (null == weakDocumentHandle) {
            // Event is not related to a known document
            return true;
        }

        DocumentHandleImpl documentHandle = weakDocumentHandle.get();
        if (event instanceof SendEvent) {
            return handle(documentHandle, (SendEvent)event);
        }
        if (event instanceof DataSourceFetchEvent) {
            return handle(documentHandle, (DataSourceFetchEvent)event);
        }
        if (event instanceof OpenURLEvent) {
            return handle(documentHandle, (OpenURLEvent)event);
        }
        if (event instanceof RefreshEvent) {
            return handle(documentHandle, (RefreshEvent) event);
        }

        // We know that the rest of the events needs to be simply executed without sending messages or callbacks.
        return true;
    }

    public void checkAndReportDataSourceErrors(Object primaryDocumentErrors) {
        for (WeakReference<DocumentHandleImpl> weakDocumentHandle : mDocumentMap.values()) {
            if (null == weakDocumentHandle) {
                continue;
            }
            final DocumentHandleImpl document = weakDocumentHandle.get();
            if (document != null) {
                DocumentConfig documentConfig = document.getDocumentConfig();
                if (documentConfig != null && documentConfig.getNativeHandle() != 0) {
                    Object errors = nGetDataSourceErrors(document.getDocumentConfig().getNativeHandle());
                    handleDataErrorRequest(document, errors);
                } else if (document.getRootContext() != null && document.getRootContext().getNativeHandle() != 0) {
                    handleDataErrorRequest(document, primaryDocumentErrors);
                } else {
                    Log.e(TAG, "Neither document config nor root context defined, hence ignoring the request");
                }
            }
        }
    }

    public void handleDataErrorRequest(final DocumentHandle document, final Object dataSourceErrors) {
        if (dataSourceErrors == null) {
            return;
        }

        MessageHandler handler = mConfig.getMessageHandler();

        if (null == handler) {
            Log.w(TAG, "No message handler, ignoring data source error(s)");
            return;
        }

        ReportRuntimeErrorRequestImpl.Payload payload = new ReportRuntimeErrorRequestImpl.Payload();
        payload.errors = (Object[]) dataSourceErrors;

        ActionMessage actionMessage =
                new ActionMessageImpl(mNextMessageId.getAndIncrement(), document,
                        "ReportRuntimeErrorRequest", payload);
        publish(() -> handler.handleAction(actionMessage));
    }

    /**
     * Tick in frame loop (bridged from RootContext)
     */
    public void tick() {
        MessageHandler handler = mConfig.getMessageHandler();
        if (null == handler) {
            return;
        }

        for (WeakReference<DocumentHandleImpl> weakHandle : mDocumentMap.values()) {
            DocumentHandleImpl handle = weakHandle.get();
            if (null == handle || !handle.isValid()) {
                continue;
            }

            if (handle.getAndClearHasVisualContextChanged()) {
                notifyVisualContextChanged(handler, handle);
            }

            if (handle.getAndClearHasDataSourceContextChanged()) {
                notifyDataSourceContextChanged(handler, handle);
            }
        }
    }

    private void notifyVisualContextChanged(MessageHandler handler, DocumentHandle handle) {
        NotificationMessage message = new NotificationMessageImpl(
                mNextMessageId.getAndIncrement(),
                handle,
                "VisualContextChanged",
                new DecodableShim());
        publish(() -> handler.handleNotification(message));
    }

    public void notifyScreenLockedChanged(DocumentHandle handle, boolean isLocked) {
        Log.d(TAG, String.format("notifyScreenLockedChanged for document: %s with isLocked: %b", handle.toString(), isLocked));
        MessageHandler messageHandler = mConfig.getMessageHandler();
        if (messageHandler == null) {
            Log.i(TAG, "No message handler defined, hence ignoring it");
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("isLocked", isLocked);
            NotificationMessage message = new NotificationMessageImpl(
                    mNextMessageId.getAndIncrement(),
                    handle,
                    "ScreenLockStatusChanged",
                    new JsonDecodable(payload));
            publish(() -> messageHandler.handleNotification(message));
        } catch (JSONException e) {
            Log.e(TAG, String.format("Unexpected failure with message %s to handle notifyScreenLocked, it will be dropped",
                    e.getMessage()));
            e.printStackTrace();
        }
    }

    public void notifyDocumentStateChanged(DocumentHandle handle, DocumentState state) {
        Log.d(TAG, String.format("notifyDocumentStateChanged for document: %s with document state: %s", handle.toString(), state.toString()));
        try {
            MessageHandler messageHandler = mConfig.getMessageHandler();
            if (messageHandler == null) {
                Log.i(TAG, "No message handler defined, hence ignoring it");
            }

            JSONObject payload = new JSONObject();
            payload.put("state", state.toString());
            NotificationMessage message = new NotificationMessageImpl(
                    mNextMessageId.getAndIncrement(),
                    handle,
                    "DocumentStateChanged",
                    new JsonDecodable(payload));
            publish(() -> messageHandler.handleNotification(message));
        } catch (JSONException e) {
            Log.e(TAG, String.format("Unexpected failure with message %s to handle DocumentStateChangedChanged, it will be dropped",
                    e.getMessage()));
            e.printStackTrace();
        }
    }

    private void notifyDataSourceContextChanged(MessageHandler handler, DocumentHandle handle) {
        NotificationMessage message = new NotificationMessageImpl(
                mNextMessageId.getAndIncrement(),
                handle,
                "DataSourceContextChanged",
                new DecodableShim());
        publish(() -> handler.handleNotification(message));
    }

    /**
     * Clean up expired documents from the map. We can forget about documents that have entered a
     * terminal state (are no longer valid). We can also forget about documents that have been
     * garbage collected.
     */
    private void cleanDocumentMap() {
        Iterator<Map.Entry<Long, WeakReference<DocumentHandleImpl>>> it =
                mDocumentMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, WeakReference<DocumentHandleImpl>> entry = it.next();
            DocumentHandle handle = entry.getValue().get();
            if (null == handle || !handle.isValid()) {
                it.remove();
            }
        }
    }

    private boolean handle(DocumentHandleImpl document, DataSourceFetchEvent event) {
        if (null == document) {
            Log.w(TAG, "Received DataSourceFetchEvent for expired document handle, ignoring it");
            return false;
        }

        MessageHandler handler = mConfig.getMessageHandler();

        if (null == handler) {
            Log.w(TAG, "Received DataSourceFetchEvent but have no message handler, ignoring it");
            return false;
        }

        Log.i(TAG, "Overriding DataSourceFetchEvent callback to route to message handler");
        event.overrideCallback(new IDataSourceFetchCallback() {
            @Override
            public void onDataSourceFetchRequest(String type, Map<String, Object> payload) {
                String dataType;
                if ("dynamicIndexList".equals(type)) {
                    dataType = "DYNAMIC_INDEX_LIST";
                } else if ("dynamicTokenList".equals(type)) {
                    dataType = "DYNAMIC_TOKEN_LIST";
                } else {
                    Log.e(TAG, String.format("Fetch request for %s type data not recognized, " +
                            "dropping it.", type));
                    return;
                }
                FetchDataRequestImpl.Payload messagePayload = new FetchDataRequestImpl.Payload();
                messagePayload.type = dataType;
                messagePayload.parameters = payload;

                ActionMessage actionMessage =
                        new ActionMessageImpl(mNextMessageId.getAndIncrement(), document,
                                FETCH_DATA_REQUEST, messagePayload);
                publish(() -> handler.handleAction(actionMessage));
            }
        });

        return true;
    }

    private boolean handle(DocumentHandleImpl document, SendEvent event) {
        if (null == document) {
            Log.w(TAG, "Received SendEvent for expired document handle, ignoring it");
            return false;
        }

        MessageHandler handler = mConfig.getMessageHandler();

        if (null == handler) {
            Log.w(TAG, "Received SendEvent but have no message handler, ignoring it");
            return false;
        }

        Log.i(TAG, "Overriding SendEvent callback to route to message handler");
        event.overrideCallback(new ISendEventCallbackV2() {
            @Override
            public void onSendEvent(Object[] args,
                                    Map<String, Object> components,
                                    Map<String, Object> sources,
                                    Map<String, Object> flags) {
                SendUserEventRequestImpl.Payload payload = new SendUserEventRequestImpl.Payload();
                payload.arguments = args;
                payload.components = components;
                payload.source = sources;
                payload.flags = flags;

                ActionMessage actionMessage =
                        new ActionMessageImpl(mNextMessageId.getAndIncrement(), document,
                                SEND_USER_EVENT_REQUEST, payload);

                publish(() -> handler.handleAction(actionMessage));
                writeAPLSessionLog(document.getSession(), Session.LogEntryLevel.INFO, String.format("%s Arguments: %s Components: %s Flags: %s Source: %s",
                        SEND_USER_EVENT_REQUEST, Arrays.toString(args), components.toString(), sources.toString(), flags ==  null ? "" : flags.toString()));
            }
        });

        return true;
    }

    /**
     * Process the content if it is a refresh event.
     * @param document  DocumentHandleImpl
     * @param event     RefreshEvent
     * @return          Boolean
     */
    private boolean handle(DocumentHandleImpl document, RefreshEvent event) {
        Log.i(TAG, "Handling refresh event");
        if (null == document) {
            Log.w(TAG, "Received RefreshEvent for expired document handle, ignoring it");
            return false;
        }
        Content content = document.getContent();
        if (content.isWaiting()) {
            Log.i(TAG, "Content is waiting, hence resolving the request");
            content.resolve(new Content.CallbackV2() {
                @Override
                public void onComplete(Content content) {
                    loadExtensionAndSetContent(document.getExtensionMediator(), document.getDocumentOptions(), content, document, true);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error occured during content resolution: " + e.getMessage());
                    document.getUserPerceivedFatalReporter().reportFatal(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE);
                }
            }, document.getRootConfig());
        }
        return content.isReady();
    }
    private boolean handle(DocumentHandleImpl document, OpenURLEvent event) {
        if (null == document) {
            Log.w(TAG, "Received OpenURL for expired document handle, ignoring it");
            return false;
        }

        MessageHandler handler = mConfig.getMessageHandler();

        if (null == handler) {
            Log.w(TAG, "Received OpenURL but have no message handler, ignoring it");
            return false;
        }

        Log.i(TAG, "Overriding OpenURL callback to route to message handler");
        event.overrideCallback((url, resultCallback) -> {
            JSONObject payload = new JSONObject();
            try {
                payload.put("source", url);
            } catch (JSONException e) {
                Log.e(TAG, String.format("Unexpected failure to handle OpenURL, it will be dropped"));
                e.printStackTrace();
                return;
            }

            ActionMessageImpl.ResponseListener listener = new ActionMessageImpl.ResponseListener() {
                @Override
                public void onSuccess(Decodable response) {
                    resultCallback.onResult(true);
                }

                @Override
                public void onFailure(String reason) {
                    Log.w(TAG, String.format("OpenURL failed: %s", reason));
                    resultCallback.onResult(false);
                }
            };
            int messageId = mNextMessageId.getAndIncrement();
            ActionMessage actionMessage = new
                    ActionMessageImpl(messageId, document, OPEN_URL_REQUEST,
                    new JsonDecodable(payload), listener);

            publish(() -> handler.handleAction(actionMessage));
            writeAPLSessionLog(document.getSession(), Session.LogEntryLevel.INFO, String.format(Locale.US ,"%s source: %s requestId: %d",
                    OPEN_URL_REQUEST, url, messageId));
        });

        return true;
    }

    private void writeAPLSessionLog(Session session, Session.LogEntryLevel level, String message) {
        if (session != null) {
            session.write(level, Session.LogEntrySource.VIEW, message);
        } else {
            Log.d(TAG, "Session is null hence skipping session.write");
        }
    }

    private static native Object nGetDataSourceErrors(long nativeHandle);

    @Override
    public void onDocumentStateChanged(DocumentState state, DocumentHandle handle) {
        Log.i(TAG, String.format("onDocumentStateChanged triggered with state: %s and handle: %s ",
                state.toString(), handle.toString()));

        DocumentHandleImpl documentHandle = (DocumentHandleImpl) handle;

        if (state == DocumentState.PREPARED) {
            inflate(documentHandle);
        }else if (state == DocumentState.DISPLAYED) {
            documentHandle.stopRenderDocumentTimer();
            mTelemetryProvider.incrementCount(mRenderDocumentCount);
        } else if (state == DocumentState.ERROR) {
            documentHandle.failRenderDocumentTimer();
            mTelemetryProvider.fail(mRenderDocumentCount);
            Log.e(TAG, String.format("Document %s moved to error state", handle));
        }
    }

    private void inflate(DocumentHandleImpl documentHandle) {
        if (!isBound()) {
            Log.i(TAG, "No view bound to Viewhost, hence request not fulfilled");
            return;
        }

        IUserPerceivedFatalCallback userPerceivedFatalCallback;
        final DocumentOptions documentOptions = documentHandle.getDocumentOptions();
        if (documentOptions != null && documentOptions.getUserPerceivedFatalCallback() != null) {
            userPerceivedFatalCallback = documentOptions.getUserPerceivedFatalCallback();
        } else {
            Log.d(TAG, "No UserPerceivedFatalCallback provided by the runtime to report UPF.");
            userPerceivedFatalCallback = new NoOpUserPerceivedFatalCallback();
        }

        APLOptions.Builder aplOptionsBuilder = APLOptions.builder()
                .telemetryProvider(mTelemetryProvider)
                .userPerceivedFatalCallback(userPerceivedFatalCallback)
                .metricsOptions(documentHandle.getMetricsOptions())
                .viewhost(this);
        if (mConfig.getTimeProvider() != null) {
            aplOptionsBuilder.timeProvider(mConfig.getTimeProvider());
        }

        APLOptions options = aplOptionsBuilder.build();

        RootConfig rootConfig = documentHandle.getRootConfig();

        if (rootConfig.getPackageManager() == null) {
            PackageManager packageManager = new PackageManager(mConfig.getIPackageLoader(), mTelemetryProvider, mDevToolsProvider.getNetworkRequestHandler());
            rootConfig.packageManager(packageManager);
        }
        rootConfig.session(documentHandle.getSession());
        mAplLayout.setAPLSession(documentHandle.getSession());
        documentHandle.getSession().setAPLListener(mDevToolsProvider.getAPLSessionListener());
        mDevToolsProvider.registerSink(options.getMetricsOptions().getMetricsSinkList());
        mDevToolsProvider.getDTView().setCurrentDocumentId(documentHandle.getSerializedId());

        if (documentHandle.getExtensionMediator() != null) {
            rootConfig.extensionMediator(documentHandle.getExtensionMediator());
        }

        if (documentHandle.getDocumentOptions() != null && documentHandle.getDocumentOptions().getEmbeddedDocumentFactory() != null)  {
            rootConfig.setDocumentManager(documentHandle.getDocumentOptions().getEmbeddedDocumentFactory() , mCoreWorker, mTelemetryProvider);
        }

        for (Map.Entry<String, Object> entry : documentHandle.getDocumentOptions().getProperties().entrySet()) {
           mProperties.set(entry.getKey(), entry.getValue());
        }

        mProperties.set(APLProperty.kFluidityRefreshRate, (double) 1000.0f / mAplLayout.getDisplayRefreshRate());

        mAplLayout.setAgentName(rootConfig);
        mAplLayout.addMetricsReadyListener(viewportMetrics -> {
            IAPLViewPresenter presenter = mAplLayout.getPresenter();
            mCoreWorker.post(() -> {
                if (!documentHandle.isValid()) {
                    Log.w(TAG, String.format("Document is no longer valid (handle:%s), dropping request to inflate document.", documentHandle));
                    return;
                }
                try {
                    final RootContext rootContext;
                    if (documentHandle.getRootContext() != null) {
                        rootContext = documentHandle.getRootContext(); // no change
                        //update the view
                        rootContext.setViewPresenter(presenter);

                        // Apply the new viewportMetrics.
                        rootContext.handleConfigurationChange(ConfigurationChange.create(viewportMetrics, rootConfig).build());
                    } else {
                        rootConfig.set(RootProperty.kInitialDisplayState, getCoreDisplayState());
                        rootContext = RootContext.create(viewportMetrics, documentHandle.getContent(), rootConfig, options, presenter, documentHandle.getUserPerceivedFatalReporter(), documentHandle.getContent().getMetricsRecorder(), mProperties);
                    }
                    // This will inflate the document.
                    documentHandle.setPrimary(rootContext, this);
                    presenter.onDocumentRender(rootContext);

                    // it will also set the state to inflated
                    documentHandle.setDocumentContext(rootContext.getDocumentContext());
                    setTopDocumentHandleAndRootContext(documentHandle, rootContext);
                } catch (Exception e) {
                    Log.e(TAG, "Exception occurred while fulfilling request and the exception is: " + e);
                    documentHandle.setDocumentState(DocumentState.ERROR);
                }
            });
            documentHandle.getContent().createDocumentBackground(viewportMetrics, rootConfig);
            if (presenter != null) {
                presenter.loadBackground(documentHandle.getContent().getDocumentBackground());
            }
        });
    }

    /**
     * Set the document reference and rootContext for the top-level document
     *
     * @param rootContext
     * @param documentHandle
     */
    void setTopDocumentHandleAndRootContext(DocumentHandle documentHandle, RootContext rootContext) {
        mTopDocument = documentHandle;
        mRootContext = rootContext;
        applyViewState(rootContext);
    }

    /**
     * Creates rootConfig from viewhostConfig
     * @return
     */
    private RootConfig createRootConfig() {
        RootConfig config = RootConfig.create();

        config.registerDataSource("dynamicIndexList");
        config.registerDataSource("dynamicTokenList");

        Map<RootProperty, Object> rootProperties = mConfig.getRootProperties() == null ? Collections.emptyMap() : mConfig.getRootProperties();
        for(RootProperty key : rootProperties.keySet()) {
            config.set(key, rootProperties.get(key));
        }

        Map<String, Object> environmentProperties = mConfig.getEnvironmentProperties() == null ? Collections.emptyMap() : mConfig.getEnvironmentProperties();
        for(String key: environmentProperties.keySet()) {
            config.set(key, environmentProperties.get(key));
        }

        if (mConfig.getAudioPlayerFactory() != null) {
            config.audioPlayerFactory(mConfig.getAudioPlayerFactory());
        }

        if (mConfig.getMediaPlayerFactory() != null) {
            config.mediaPlayerFactory(mConfig.getMediaPlayerFactory());
        }


        return config;
    }
}
