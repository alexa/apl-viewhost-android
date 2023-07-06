/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionMediator;
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Internal implementation of the document handle
 */
class DocumentHandleImpl extends DocumentHandle {
    private static final String TAG = "DocumentHandleImpl";
    private Queue<ExecuteCommandsRequest> mExecuteCommandsRequestQueue= new LinkedList<>();
    private Handler mCoreWorker;
    private Collection<DocumentStateChangeListener> mDocumentStateChangeListeners;
    private Content mContent;
    private DocumentState mDocumentState;
    private ExtensionMediator mExtensionMediator;
    private DocumentOptions mDocumentOptions;
    /**
     * Retain a link to core's DocumentContext. It can be null while the document is being prepared
     * and the core document context hasn't been created yet.
     */
    @Nullable
    private DocumentContext mDocumentContext;
    @Nullable
    private DocumentConfig mDocumentConfig;

    /**
     * Retain only a weak reference to the viewhost, since that is the parent object that creates
     * document handles. We want avoid circular references.
     */
    private final WeakReference<ViewhostImpl> mViewhost;

    DocumentHandleImpl(ViewhostImpl viewhost, Handler coreWorker) {
        mViewhost = new WeakReference<ViewhostImpl>(viewhost);
        mCoreWorker = coreWorker;
        mDocumentState = DocumentState.PENDING;
        mDocumentStateChangeListeners = new LinkedList<>();
        mContent = null;
        mExtensionMediator = null;
        mDocumentOptions = null;
    }

    @Override
    public boolean isValid() {
        return !(DocumentState.FINISHED.equals(mDocumentState) || DocumentState.ERROR.equals(mDocumentState));
    }

    @Override
    public String getToken() {
        return "token";
    }

    @Override
    public String getUniqueId() {
        return "1234-ABCD";
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

    @Nullable
    public Content getContent(){
        return mContent;
    }

    @Nullable
    public ExtensionMediator getExtensionMediator(){
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

    @Override
    public boolean executeCommands(ExecuteCommandsRequest request) {
        if (!isValid()) {
            return false;
        }
        String commands = ((JsonStringDecodable)request.getCommands()).getString();
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

        if (getDocumentConfig() == null || getDocumentConfig().getNativeHandle() == 0) {
            Log.e(TAG, "DocumentConfig handle 0 which means provider not defined in document options, hence ignoring the update data source request");
            return false;
        }

        ViewhostImpl viewhost = mViewhost.get();
        UpdateDataSourceCallback callback = request.getCallback();
        mCoreWorker.post(() -> {
            try {
                String payload = ((JsonStringDecodable)request.getData()).getString();
                JSONObject jsonObject = new JSONObject(payload);
                if (!jsonObject.has("type")) {
                    Log.e(TAG, "Data Source type not defined, hence update failed");
                    if (callback != null) {
                        viewhost.publish(() -> {
                            callback.onFailure("Data Source type not defined, hence update failed");
                        });
                    }
                } else {
                    String type = jsonObject.getString("type");
                    boolean updated = nUpdateDataSource(type,  payload, getDocumentConfig().getNativeHandle());
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
        mDocumentState = state;
        // update all the listeners with the new state
        for (DocumentStateChangeListener listener : mDocumentStateChangeListeners) {
            listener.onDocumentStateChanged(state);
        }
        final ViewhostImpl viewhost = mViewhost.get();
        if (viewhost != null) {
            viewhost.notifyDocumentStateChanged(this, state);
        }
    }

    public void registerStateChangeListener(DocumentStateChangeListener listener) {
        mDocumentStateChangeListeners.add(listener);
        // the listener has no state set it to the state of the handler
        listener.onDocumentStateChanged(mDocumentState);
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


    @Override
    public boolean finish(FinishDocumentRequest request) {
        return false;
    }

    public DocumentContext getDocumentContext() {
        return mDocumentContext;
    }

    private static native boolean nUpdateDataSource(String type, String payload, long documentConfigNativeHandle);
}
