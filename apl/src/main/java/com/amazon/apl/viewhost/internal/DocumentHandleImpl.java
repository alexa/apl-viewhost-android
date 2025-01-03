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
import com.amazon.apl.devtools.util.IdGenerator;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Internal implementation of the document handle
 */
public class DocumentHandleImpl extends DocumentHandle {
    private static final String TAG = "DocumentHandleImpl";
    private static final IdGenerator mSerializeDocIdGenerator = new IdGenerator();
    private final int mSerializedDocId;
    private Queue<ExecuteCommandsRequest> mExecuteCommandsRequestQueue = new LinkedList<>();
    private Handler mCoreWorker;
    private Set<DocumentStateChangeListener> mDocumentStateChangeListeners;
    private Content mContent;

    private final ConcurrentLinkedQueue<DocumentSettingsCallback> mDocumentSettingsCallbackQueue;
    private DocumentState mDocumentState;
    private ExtensionMediator mExtensionMediator;
    private DocumentOptions mDocumentOptions;
    private ITelemetryProvider mTelemetryProvider = NoOpTelemetryProvider.getInstance();
    private String mToken;
    // Pending tasks that should execute post-inflation on the core worker
    private final ConcurrentLinkedQueue<Runnable> mPendingTasks = new ConcurrentLinkedQueue<>();

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
        mSerializedDocId = mSerializeDocIdGenerator.generateId();
        Log.i(TAG, "Document UniqueID is: " + mUniqueID);
        mDocumentSettingsCallbackQueue = new ConcurrentLinkedQueue<>();
        registerStateChangeListener(new DocumentStateChangedWrapper());
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

    /**
     * @return the serialized unique id corresponding to this Document.
     */
    public int getSerializedId() {
        return mSerializedDocId;
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
        Log.i(TAG, String.format("Request received to execute commands for document:%s and the commands are:%s", this, commands));

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

    /**
     * Convenience wrapper that gracefully handles a null callback and also logs on failure.
     */
    static class UpdateDataSourceCallbackWrapper implements UpdateDataSourceCallback {
        private UpdateDataSourceCallback mCallback;
        UpdateDataSourceCallbackWrapper(UpdateDataSourceCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onSuccess() {
            if (mCallback != null) {
                mCallback.onSuccess();
            }
        }

        @Override
        public void onFailure(String reason) {
            Log.w(TAG, "Data source update failed: " + reason);
            if (mCallback != null) {
                mCallback.onFailure(reason);
            }
        }
    }

    @Override
    public boolean updateDataSource(UpdateDataSourceRequest request) {
        if (!isValid()) {
            Log.w(TAG, "Cannot update data source as document is no longer valid");
            return false;
        }

        Log.i(TAG, String.format("Request received to updateDataSource for document:%s", this));

        executeWithDocumentContext(() -> {
            UpdateDataSourceCallback callback = new UpdateDataSourceCallbackWrapper(request.getCallback());
            ViewhostImpl viewhost = mViewhost.get();
            if (viewhost == null) {
                Log.w(TAG, "View host is gone, dropping request to update data source");
                return;
            }

            if (!isValid()) {
                viewhost.publish(() -> callback.onFailure("Document became invalid"));
                return;
            }

            String payload = ((JsonStringDecodable) request.getData()).getString();
            String type;
            try {
                JSONObject jsonObject = new JSONObject(payload);
                type = jsonObject.has("type") ? jsonObject.getString("type") : request.getType();
            } catch (JSONException ex) {
                viewhost.publish(() -> callback.onFailure("JSON parsing error occurred, " + ex.getMessage()));
                return;
            }
            if (TextUtils.isEmpty(type)) {
                viewhost.publish(() -> callback.onFailure("Data Source type not defined"));
                return;
            }

            // At this point we either have a mRootContext (primary document) or we have a document
            // config (embedded document). We need one or the other in order to call into core.
            if (mRootContext == null && (mDocumentConfig == null || mDocumentConfig.getNativeHandle() == 0)) {
                Log.e(TAG, "RootContext null or DocumentConfig handle 0 which means provider not defined in document options, hence ignoring the update data source request");
                viewhost.publish(() -> callback.onFailure("Internal failure"));
                return;
            }

            boolean updated = mRootContext != null ?
                mRootContext.updateDataSource(type, payload) : nUpdateDataSource(type, payload, mDocumentConfig.getNativeHandle());
            if (updated) {
                viewhost.publish(() -> callback.onSuccess());
            } else {
                viewhost.publish(() -> callback.onFailure("Encountered runtime error in processing data update"));
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

        final boolean wasValid = isValid();
        mDocumentState = state;
        // update all the listeners with the new state
        for (DocumentStateChangeListener listener : mDocumentStateChangeListeners) {
            listener.onDocumentStateChanged(state, this);
        }
        final ViewhostImpl viewhost = mViewhost.get();
        if (viewhost != null) {
            viewhost.notifyDocumentStateChanged(this, state);
        }
        if (wasValid && !isValid()) {
            // Document became invalid, so execute pending tasks so that
            // anything waiting for a document context can fail gracefully
            executePendingTasks();
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
        executePendingTasks();
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
        mViewhost = new WeakReference<>(viewhost);
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
        Log.i(TAG, String.format("Request received to finish document:%s", this));
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
                mExtensionMediator.finish();
            }
            mRootContext = null;
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
    public boolean requestDocumentSettings(DocumentSettingsCallback callback) {
        ViewhostImpl viewhost = mViewhost.get();
        if (null == viewhost) {
            Log.w(TAG, "Could not respond to requestDocumentSettings, view host instance is gone.");
            return false;
        }
        if (!isValid()) {
            viewhost.publish(() -> callback.onFailure("Document is no longer valid"));
            return false;
        }
        if (areSettingsAvailable(this.getDocumentState())) {
            publishSettings(callback, viewhost);
        } else {
            mDocumentSettingsCallbackQueue.add(callback);
        }
        return true;
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

    /**
     * Internal class to handle state change events and route messages to document settings callback.
     */
    private class DocumentStateChangedWrapper implements DocumentStateChangeListener {
        @Override
        public void onDocumentStateChanged(DocumentState state, DocumentHandle handle) {
            if (DocumentState.PENDING.equals(state)) {
                return;
            }

            ViewhostImpl viewhost = mViewhost.get();
            if (null == viewhost) {
                Log.w(TAG, "Could not respond to state change events, view host instance is gone.");
                return;
            }

            for (DocumentSettingsCallback callback = mDocumentSettingsCallbackQueue.poll(); callback != null; callback = mDocumentSettingsCallbackQueue.poll()) {
                if (areSettingsAvailable(state)) {
                    publishSettings(callback, viewhost);
                } else if (!isValid()) {
                    final DocumentSettingsCallback documentSettingsCallback = callback;
                    viewhost.publish(() -> documentSettingsCallback.onFailure("Document not valid anymore"));

                }
            }
        }
    }

    private void publishSettings(final DocumentSettingsCallback callback, final ViewhostImpl viewhost) {
        viewhost.publish(() -> {
            try {
                callback.onSuccess(mContent.getSerializedDocumentSettings(mRootConfig));
            } catch (JSONException e) {
                viewhost.publish(() -> callback.onFailure("unexpected serialization failure"));
                Log.wtf(TAG, "Error serializing document settings object.", e);
            }
        });
    }

    boolean areSettingsAvailable(DocumentState state) {
        return DocumentState.PREPARED.equals(state) || DocumentState.INFLATED.equals(state)
                || DocumentState.DISPLAYED.equals(state);
    }

    private void executeWithDocumentContext(Runnable task) {
        mCoreWorker.post(() -> {
            if (null == mDocumentContext) {
                Log.i(TAG, String.format("Adding post-inflation updateDataSource request for document:%s", this));
                mPendingTasks.add(task);
                return;
            }

            // Just run it now
            task.run();
        });
    }

    private void executePendingTasks() {
        while (!mPendingTasks.isEmpty()) {
            mCoreWorker.post(mPendingTasks.poll());
        }
    }
}
