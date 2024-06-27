/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Session;
import com.amazon.apl.android.UserPerceivedFatalReporter;
import com.amazon.apl.android.dependencies.impl.NoOpUserPerceivedFatalCallback;
import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest.ExecuteCommandsCallback;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest.UpdateDataSourceCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Internal implementation of the document handle
 */
public class DocumentHandleImpl extends DocumentHandle {
    private static final String TAG = "DocumentHandleImpl";
    private Queue<ExecuteCommandsRequest> mExecuteCommandsRequestQueue = new LinkedList<>();
    private Handler mCoreWorker;
    private Set<DocumentStateChangeListener> mDocumentStateChangeListeners;
    private Content mContent;
    private DocumentState mDocumentState;
    private ExtensionMediator mExtensionMediator;
    private DocumentOptions mDocumentOptions;
    private ITelemetryProvider mTelemetryProvider = NoOpTelemetryProvider.getInstance();
    private String mToken;

    /**
     * Retain a link to core's DocumentContext. It can be null while the document is being prepared
     * and the core document context hasn't been created yet.
     */
    @Nullable
    private DocumentContext mDocumentContext;

    /**
     * Root Context only for primary document. Null for embedded docs.
     */
    @Nullable
    private RootContext mRootContext;
    @Nullable
    private DocumentConfig mDocumentConfig;
    @Nullable
    private RootConfig mRootConfig;
    @Nullable
    private UserPerceivedFatalReporter mUserPerceivedFatalReporter;

    /**
     * Retain only a weak reference to the viewhost, since that is the parent object that creates
     * document handles. We want avoid circular references.
     */
    private WeakReference<ViewhostImpl> mViewhost;
    private final long mDocumentCreationTime;

    private static final String RENDER_DOCUMENT_TAG = "Viewhost." + ITelemetryProvider.RENDER_DOCUMENT;
    private Integer mRenderDocumentTimer = ITelemetryProvider.UNKNOWN_METRIC_ID;
    private final String mUniqueID;

    private final Session mSession;

    private final MetricsOptions mMetricsOptions;

    DocumentHandleImpl(final ViewhostImpl viewhost,
                       final Handler coreWorker,
                       final MetricsOptions metricsOptions) {
        mViewhost = new WeakReference<>(viewhost);
        mCoreWorker = coreWorker;
        mDocumentState = DocumentState.PENDING;
        mDocumentStateChangeListeners = new HashSet<>();
        mContent = null;
        mExtensionMediator = null;
        mDocumentOptions = null;
        mDocumentCreationTime = SystemClock.elapsedRealtime();
        mUniqueID = UUID.randomUUID().toString();
        mUserPerceivedFatalReporter = new UserPerceivedFatalReporter(new NoOpUserPerceivedFatalCallback());
        mSession = new Session();
        mMetricsOptions = metricsOptions;
        Log.d(TAG, "Document UniqueID is: " + mUniqueID);
    }

    public Handler getCoreWorker() {
        return mCoreWorker;
    }

    public Session getSession() {
        return mSession;
    }

    /**
     * Start time for prepare document call.
     *
     * @return
     */
    public long getPrepareDocumentStartTime() {
        return mDocumentCreationTime;
    }

    /**
     * @return {@link MetricsOptions} specific for this {@link DocumentHandleImpl}
     */
    public MetricsOptions getMetricsOptions() {
        return mMetricsOptions;
    }

    @Override
    public boolean isValid() {
        return !(DocumentState.FINISHED.equals(mDocumentState) || DocumentState.ERROR.equals(mDocumentState));
    }

    @Override
    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        mToken = token;
    }

    @Override
    public String getUniqueId() {
        return mUniqueID;
    }

    @Override
    public boolean requestVisualContext(VisualContextCallback callback) {
        if (!isValid()) {
            Log.w(TAG, "Could not serialize visual context, document is no longer valid.");
            return false;
        }

        ViewhostImpl viewhost = mViewhost.get();
        if (null == viewhost) {
            Log.w(TAG, "Could not serialize visual context, view host instance is gone.");
            return false;
        }

        mCoreWorker.post(() -> {
            if (null == mDocumentContext) {
                Log.w(TAG, "Could not serialize visual context, document not available.");
                viewhost.publish(() -> callback.onFailure("document not available"));
                return;
            }
            try {
                String serialized = mDocumentContext.serializeVisualContext();
                Decodable decodable = new JsonDecodable(new JSONObject(serialized));
                viewhost.publish(() -> callback.onSuccess(decodable));
            } catch (JSONException e) {
                viewhost.publish(() -> callback.onFailure("unexpected serialization failure"));
                Log.wtf(TAG, "Error serializing visual context object.", e);
            }
        });
        return true;
    }

    @Override
    public boolean requestDataSourceContext(DataSourceContextCallback callback) {
        if (!isValid()) {
            Log.w(TAG, "Could not serialize data source context, document is no longer valid.");
            return false;
        }

        ViewhostImpl viewhost = mViewhost.get();
        if (null == viewhost) {
            Log.w(TAG, "Could not serialize data source context, view host instance is gone.");
            return false;
        }

        mCoreWorker.post(() -> {
            if (null == mDocumentContext) {
                Log.w(TAG, "Could not serialize data source context, document not available.");
                viewhost.publish(() -> callback.onFailure("document not available"));
                return;
            }
            try {
                String serialized = mDocumentContext.serializeDataSourceContext();
                Decodable decodable = new JsonDecodable(new JSONArray(serialized));
                viewhost.publish(() -> callback.onSuccess(decodable));
            } catch (JSONException e) {
                viewhost.publish(() -> callback.onFailure("unexpected serialization failure"));
                Log.wtf(TAG, "Error serializing data source context object.", e);
            }
        });
        return true;
    }
    @Override
    public void cancelExecution() {
        Log.d(TAG, "Received cancelExecution for handle: " + this);
        if(mRootContext == null) {
            Log.w(TAG,"RootContext is null, skipping cancelExecution.");
            return;
        }
        mRootContext.cancelExecution();
    }

    @Nullable
    public Content getContent() {
        return mContent;
    }

    @Nullable
    public ExtensionMediator getExtensionMediator() {
        return mExtensionMediator;
    }

    public void setExtensionMediator(ExtensionMediator mediator) {
        mExtensionMediator = mediator;
    }

    @Nullable
    public DocumentOptions getDocumentOptions() {
        return mDocumentOptions;
    }

    public void setDocumentOptions(DocumentOptions documentOptions) {
        mDocumentOptions = documentOptions;
    }

    public void setUserPerceivedFatalReporter(UserPerceivedFatalReporter userPerceivedFatalReporter) {
        mUserPerceivedFatalReporter = userPerceivedFatalReporter;
    }
    public UserPerceivedFatalReporter getUserPerceivedFatalReporter() {
        return mUserPerceivedFatalReporter;
    }

    @Override
    public boolean executeCommands(ExecuteCommandsRequest request) {
        if (!isValid()) {
            return false;
        }
        String commands = ((JsonStringDecodable) request.getCommands()).getString();
        Log.d(TAG, String.format("Running commands %s for host component", commands));

        if (mViewhost.get() == null) {
            Log.e(TAG, "viewhost not initialised");
            return false;
        }

        if (mDocumentContext == null) {
            Log.e(TAG, "executeCommands: documentContext is null hence queuing the request");
            mExecuteCommandsRequestQueue.add(request);
            return false;
        }

        ViewhostImpl viewhost = mViewhost.get();
        mCoreWorker.post(() -> {
            Action action = mDocumentContext.executeCommands(commands);
            ExecuteCommandsCallback callback = request.getCallback();
            if (callback != null) {
                if (action != null) {
                    action.then(() -> {
                        viewhost.publish(() -> {
                            callback.onComplete();
                        });
                    });
                    action.addTerminateCallback(() -> {
                        viewhost.publish(() -> {
                            callback.onTerminated();
                        });
                    });
                } else {
                    viewhost.publish(() -> {
                        callback.onComplete();
                    });
                }
            }
        });
        return true;
    }

    @Override
    public boolean updateDataSource(UpdateDataSourceRequest request) {
        if (!isValid()) {
            Log.w(TAG, "Cannot update data source as document is no longer valid");
            return false;
        }

        if ((mRootContext == null ) && (getDocumentConfig() == null || getDocumentConfig().getNativeHandle() == 0)) {
            Log.e(TAG, "RootContext null or DocumentConfig handle 0 which means provider not defined in document options, hence ignoring the update data source request");
            return false;
        }

        ViewhostImpl viewhost = mViewhost.get();
        UpdateDataSourceCallback callback = request.getCallback();
        mCoreWorker.post(() -> {
            try {
                String payload = ((JsonStringDecodable) request.getData()).getString();
                JSONObject jsonObject = new JSONObject(payload);
                String type = jsonObject.has("type") ? jsonObject.getString("type") : request.getType();
                if (TextUtils.isEmpty(type)) {
                    Log.e(TAG, "Data Source type not defined, hence update failed");
                    if (callback != null) {
                        viewhost.publish(() -> {
                            callback.onFailure("Data Source type not defined, hence update failed");
                        });
                    }
                } else {
                    boolean updated = mRootContext != null ? mRootContext.updateDataSource(type, payload) : nUpdateDataSource(type, payload, getDocumentConfig().getNativeHandle());
                    if (callback != null) {
                        if (updated) {
                            viewhost.publish(() -> {
                                callback.onSuccess();
                            });
                        } else {
                            viewhost.publish(() -> {
                                callback.onFailure("Encountered runtime error in processing data update");
                            });
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.e(TAG, String.format("JSON exception occurred with message %s, hence ignoring the update request", ex.getMessage()));
                if (callback != null) {
                    viewhost.publish(() -> {
                        callback.onFailure("JSON parsing error occurred, hence ignoring the update request");
                    });
                }
            }
        });
        return true;
    }

    @Override
    public synchronized boolean setUserData(Object data) {
        if (!isValid()) {
            return false;
        }
        return super.setUserData(data);
    }

    public void setDocumentState(DocumentState state) {
        Log.d(TAG, String.format("setDocumentState for handle: %s and new state: %s", this, state.toString()));
        mDocumentState = state;
        // update all the listeners with the new state
        for (DocumentStateChangeListener listener : mDocumentStateChangeListeners) {
            listener.onDocumentStateChanged(state, this);
        }
        final ViewhostImpl viewhost = mViewhost.get();
        if (viewhost != null) {
            viewhost.notifyDocumentStateChanged(this, state);
        }
    }

    public void registerStateChangeListener(DocumentStateChangeListener listener) {
        if (!mDocumentStateChangeListeners.contains(listener)) {
            mDocumentStateChangeListeners.add(listener);
            // the listener has no state set it to the state of the handler
            listener.onDocumentStateChanged(mDocumentState, this);
        }
    }

    public void setContent(Content content) {
        mContent = content;
        setDocumentState(DocumentState.PREPARED);
    }

    /**
     * @return true if the visual context has changed from clean to dirty
     */
    public boolean getAndClearHasVisualContextChanged() {
        if (!isValid() || mDocumentContext == null) {
            return false;
        }
        return mDocumentContext.getAndClearHasVisualContextChanged();
    }

    /**
     * @return true if the data source context has changed from clean to dirty
     */
    public boolean getAndClearHasDataSourceContextChanged() {
        if (!isValid() || mDocumentContext == null) {
            return false;
        }
        return mDocumentContext.getAndClearHasDataSourceContextChanged();
    }


    /**
     * 1. Update the document handle's link to the core DocumentContext.
     * 2. Polls any pending execute command requests and executes them.
     *
     * @param documentContext
     */
    public void setDocumentContext(DocumentContext documentContext) {
        mDocumentContext = documentContext;
        setDocumentState(DocumentState.INFLATED);
        // Notify the viewhost of the document handle's association with a document context
        final ViewhostImpl viewhost = mViewhost.get();
        if (viewhost != null) {
            viewhost.updateDocumentMap(this);
        }

        pollRequests();
    }

    /**
     * Designate this as a primary (top-level) document, associating it with a
     * RootContext as well as the viewhost in which it was rendered.
     *
     * This is done in order to:
     *
     * 1. Wire in DocumentHandle actions that that operate specifically on the
     *    primary document such as cancelExecution() and finishDocument().
     * 2. Start publishing all notifications concerning this document to the
     *    specified view host. This is to allow a document to be prepared on one
     *    viewhost and then rendered on another.
     *
     * @param rootContext  The RootContext associated with this document.
     * @param viewhost     The viewhost to which notifications should be published
     */
    public void setPrimary(RootContext rootContext, ViewhostImpl viewhost) {
        mRootContext = rootContext;
        mRootContext.setDocumentHandle(this);
        DocumentContext documentContext = rootContext.getDocumentContext();
        mViewhost = new WeakReference<>(viewhost);
        setDocumentContext(documentContext);
    }

    /**
     * Set the telemetry provider for this instance
     * @param telemetryProvider
     */
    public void setTelemetryProvider(@NonNull ITelemetryProvider telemetryProvider) {
        mTelemetryProvider = telemetryProvider;
    }

    /**
     * poll any pending execute command requests.
     */
    private void pollRequests() {
        while (!mExecuteCommandsRequestQueue.isEmpty()) {
            executeCommands(mExecuteCommandsRequestQueue.poll());
        }
    }

    @Nullable
    public DocumentConfig getDocumentConfig() {
        return mDocumentConfig;
    }

    public void setDocumentConfig(@Nullable DocumentConfig documentConfig) {
        this.mDocumentConfig = documentConfig;
    }

    private boolean isTokenValid(String token) {
        // If not specified, then we will not attempt to match the token
        if (token == null) {
            return true;
        }

        // If specified, then the specified token must match
        return token.equals(mToken);
    }

    @Override
    public boolean finish(FinishDocumentRequest request) {
        if (!isValid()) {
            Log.w(TAG, "Could not finish, document is no longer valid.");
            return false;
        }
        if (!isTokenValid(request.getToken())) {
            Log.w(TAG, "Ignoring finish request due to mismatched tokens.");
            return false;
        }
        mExecuteCommandsRequestQueue.clear();
        mCoreWorker.post(() -> {
            if (mRootContext != null) {
                try {
                    mRootContext.finishDocument();
                } catch (Exception e) {
                    Log.e(TAG, "Exception while fulfilling request and the exception is: " + e);
                    setDocumentState(DocumentState.ERROR);
                }
            } else {
                //when the DocumentState is pending or prepared, but not yet rendered
                setDocumentState(DocumentState.FINISHED);
            }
            if (mExtensionMediator != null) {
                mExtensionMediator.enable(false);
            }
        });
        return true;
    }

    void startRenderDocumentTimer(TimeUnit timeUnit, long initialElapsedTime) {
        if (mRenderDocumentTimer != ITelemetryProvider.UNKNOWN_METRIC_ID) {
            Log.w(TAG, "Attempting to start renderDocument but it is already running");
            return;
        }

        mRenderDocumentTimer = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, RENDER_DOCUMENT_TAG, ITelemetryProvider.Type.TIMER);
        mTelemetryProvider.startTimer(mRenderDocumentTimer, timeUnit, initialElapsedTime);
    }

    void failRenderDocumentTimer() {
        if (mRenderDocumentTimer == ITelemetryProvider.UNKNOWN_METRIC_ID) {
            Log.w(TAG, "Attempting to fail renderDocument before starting");
            return;
        }

        mTelemetryProvider.fail(mRenderDocumentTimer);
        mRenderDocumentTimer = ITelemetryProvider.UNKNOWN_METRIC_ID;
    }

    void stopRenderDocumentTimer() {
        if (mRenderDocumentTimer == ITelemetryProvider.UNKNOWN_METRIC_ID) {
            Log.w(TAG, "Attempting to stop renderDocument before starting");
            return;
        }

        mTelemetryProvider.stopTimer(mRenderDocumentTimer);
        mRenderDocumentTimer = ITelemetryProvider.UNKNOWN_METRIC_ID;
    }

    @Override
    public <K> K getDocumentSetting(String propertyName, K defaultValue) {
        if (mContent != null) {
            return mContent.optSetting(propertyName, defaultValue);
        } else {
            Log.w(TAG, "Content is null, returning defaultValue");
            return defaultValue;
        }
    }

    @Override
    public String toString() {
        return "DocumentHandle{"
                + "id=" + getUniqueId() + ", "
                + "state=" + mDocumentState.toString() + ", "
                + "token=" + getToken()
                + "}";
    }

    public DocumentState getDocumentState() {
        return mDocumentState;
    }

    public DocumentContext getDocumentContext() {
        return mDocumentContext;
    }

    public RootContext getRootContext() {
        return mRootContext;
    }

    @Nullable
    public RootConfig getRootConfig() {
        return mRootConfig;
    }

    public void setRootConfig(@Nullable RootConfig rootConfig) {
        mRootConfig = rootConfig;
    }

    private static native boolean nUpdateDataSource(String type, String payload, long documentConfigNativeHandle);
}
