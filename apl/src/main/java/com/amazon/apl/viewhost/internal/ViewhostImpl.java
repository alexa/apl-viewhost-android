/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.Session;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.events.DataSourceFetchEvent;
import com.amazon.apl.android.events.OpenURLEvent;
import com.amazon.apl.android.events.SendEvent;
import com.amazon.apl.android.thread.Threading;
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
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal implementation of the viewhost
 */
public class ViewhostImpl extends Viewhost {
    private static final String TAG = "ViewhostImpl";
    private final ViewhostConfig mConfig;
    private final Executor mRuntimeInteractionWorker;
    private final Handler mCoreWorker;
    private final AtomicInteger mNextMessageId;
    private static final String EMPTY = "";

    /**
     * Maintain a map of documents that are known to the new viewhost. This is needed for event
     * routing.
     */
    private final Map<Long, WeakReference<DocumentHandleImpl>> mDocumentMap;

    public ViewhostImpl(ViewhostConfig config, Executor runtimeInteractionWorker, Handler coreWorker) {
        mConfig = config;
        mDocumentMap = new HashMap<>();
        mRuntimeInteractionWorker = runtimeInteractionWorker;
        mCoreWorker = coreWorker;
        mNextMessageId = new AtomicInteger(1);
    }

    public ViewhostImpl(ViewhostConfig config) {
        this(config, Threading.createSequentialExecutor(), new Handler(Looper.getMainLooper()));
    }

    @Override
    public PreparedDocument prepare(final PrepareDocumentRequest request) {
        //For first iteration of M1 milestone we are keeping the content creation in this block. May move it later
        String document = request.getDocument() != null ? ((JsonStringDecodable) request.getDocument()).getString() : EMPTY;
        APLOptions options = createAPLOptions(request);

        ExtensionRegistrar registrar = mConfig.getExtensionRegistrar();
        final ExtensionMediator mediator = (registrar != null) ? ExtensionMediator.create(registrar, DocumentSession.create()) : null;
        DocumentHandleImpl handle = new DocumentHandleImpl(this, mCoreWorker);
        handle.setExtensionMediator(mediator);
        Content.create(document, options, new Content.CallbackV2() {

            @Override
            public void onComplete(Content content) {
                DocumentOptions documentOptions = request.getDocumentOptions() != null ? request.getDocumentOptions() : mConfig.getDefaultDocumentOptions();

                if (documentOptions != null) {
                    handle.setDocumentOptions(documentOptions);
                }
                if (mediator != null && documentOptions != null && documentOptions.getExtensionGrantRequestCallback() != null) {
                    Map<String, Object> flags = documentOptions.getExtensionFlags() != null ? documentOptions.getExtensionFlags() : new HashMap<>();
                    mediator.initializeExtensions(flags, content, documentOptions.getExtensionGrantRequestCallback());
                    mediator.loadExtensions(flags, content, () -> {
                        // attach the completed content to the DocumentHandle
                        handle.setContent(content);
                    });
                } else {
                    handle.setContent(content);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.i(TAG, e.toString());
                handle.setDocumentState(DocumentState.ERROR);
            }

            @Override
            public void onPackageLoaded(Content content) {
                // do nothing when packages are loaded
            }
        }, new Session());
        return new PreparedDocumentImpl(handle);
    }


    private APLOptions createAPLOptions(final PrepareDocumentRequest request) {
        // Since Data is optional, if it is not included, this will be serialized into a null content in APLOptions,
        // and ignored later in the content creation process
        String data = request.getData() != null ? ((JsonStringDecodable) request.getData()).getString() : EMPTY;
        // In order to re-use the existing Content::Create methods creating an APLOptions instance here
        // This wil be eventually phased out in favor of other unified Viewhost APIs
        IPackageLoader packageLoader = mConfig.getIPackageLoader() == null ? (importRequest, successCallback, failureCallback) -> failureCallback.onFailure(importRequest, "Content package loading not implemented.") : mConfig.getIPackageLoader();
        APLOptions options = APLOptions.builder()
                .packageLoader(packageLoader)
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
                    String result;
                    if (currentData.has(source)) {
                        result = currentData.optString(source);
                    } else {
                        result = currentData.toString();
                    }
                    successCallback.onSuccess(source, result);
                })
                .build();
        return options;
    }

    @Override
    public DocumentHandle render(RenderDocumentRequest request) {
        return null;
    }

    @Override
    public DocumentHandle render(PreparedDocument preparedDocument) {
        return null;
    }

    public void publish(Runnable task) {
        mRuntimeInteractionWorker.execute(task);
    }

    /**
     * Internal method for notifying viewhost about document context assignments
     * @param handle The document handle
     */
    public void updateDocumentMap(DocumentHandle handle) {
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

        // We know this event is related to a document we're managing, so we drop it
        return false;
    }

    public void checkAndReportDataSourceErrors() {
        for (WeakReference<DocumentHandleImpl> weakDocumentHandle : mDocumentMap.values()) {
            if (null == weakDocumentHandle) {
                continue;
            }
            final DocumentHandleImpl document = weakDocumentHandle.get();
            if (document != null) {
                DocumentConfig documentConfig = document.getDocumentConfig();;
                if (documentConfig == null && documentConfig.getNativeHandle() == 0) {
                    Log.e(TAG, "document config does not exist, hence ignoring the request");
                }
                Object errors = nGetDataSourceErrors(document.getDocumentConfig().getNativeHandle());
                handleDataErrorRequest(document, errors);
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
        mRuntimeInteractionWorker.execute(() -> handler.handleAction(actionMessage));
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

    public void notifyDocumentStateChanged(DocumentHandle handle, DocumentState state) {
        Log.d(TAG, String.format("notifyDocumentStateChanged for document state: %s", state.toString()));
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
                                "FetchDataRequest", messagePayload);
                mRuntimeInteractionWorker.execute(() -> handler.handleAction(actionMessage));
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
                                "SendUserEventRequest", payload);

                mRuntimeInteractionWorker.execute(() -> handler.handleAction(actionMessage));
            }
        });

        return true;
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

            ActionMessage actionMessage = new
                    ActionMessageImpl(mNextMessageId.getAndIncrement(), document, "OpenURLRequest",
                    new JsonDecodable(payload), listener);

            mRuntimeInteractionWorker.execute(() -> handler.handleAction(actionMessage));
        });

        return true;
    }
    private static native Object nGetDataSourceErrors(long nativeHandle);
}
