/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models;

import android.graphics.Bitmap;
import android.os.Handler;

import android.view.View;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.LiveData;
import com.amazon.apl.android.dependencies.IAPLSessionListener;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;
import com.amazon.apl.android.utils.MetricInfo;
import com.amazon.apl.developer.views.CaptureImageHelper;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.enums.TargetType;
import com.amazon.apl.devtools.enums.ViewState;
import com.amazon.apl.devtools.models.log.LogEntry;
import com.amazon.apl.devtools.models.log.LogEntryAddedEvent;
import com.amazon.apl.devtools.models.network.DTNetworkRequestHandler;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.devtools.models.network.NetworkLoadingFailedEvent;
import com.amazon.apl.devtools.models.network.NetworkLoadingFinishedEvent;
import com.amazon.apl.devtools.models.network.NetworkRequestWillBeSentEvent;
import com.amazon.apl.devtools.models.performance.PerformanceMetricsEvent;
import com.amazon.apl.devtools.models.view.ExecuteCommandStatus;
import com.amazon.apl.devtools.models.view.ViewStateChangeEvent;
import com.amazon.apl.devtools.util.DependencyContainer;

import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.IdGenerator;
import com.amazon.apl.devtools.util.RequestStatus;
import com.amazon.apl.devtools.views.IAPLView;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewTypeTarget is a Target that contains a single Android view.
 */
public final class ViewTypeTarget extends Target implements IAPLSessionListener {
    private IAPLView mView;
    private final Handler mHandler;
    private final List<LogEntry> mLogEntries = new ArrayList<>();
    private final IdGenerator mIdGenerator = new IdGenerator();
    private final IDTNetworkRequestHandler mDTNetworkRequestHandler;
    private int mCurrentDocumentId = 0;
    private ViewState mCurrentDocumentView = ViewState.EMPTY;
    private APLOptions mAPLOptions;

    private final CaptureImageHelper mCaptureImageHelper = new CaptureImageHelper();

    public ViewTypeTarget(String name) {
        super(TargetType.VIEW, name );
        mHandler = DependencyContainer.getInstance().getTargetCatalog().getHandler();
        mDTNetworkRequestHandler = new DTNetworkRequestHandler(this);
    }

    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    public void setView(IAPLView aplViewLayout) {
        mView = aplViewLayout;
    }

    public void addLiveData(String name, LiveData data) {
        mView.addLiveData(name, data);
    }


    @Override
    public void registerSession(Session session) {
        super.registerSession(session);
        post(() -> onViewStateChange(mCurrentDocumentView, System.currentTimeMillis() / 1000D));
    }

    public void setDocument(String aplDocument, String aplDocumentData) {
        mView.renderAPLDocument(aplDocument, aplDocumentData);
    }

    public void setAPLOptions(APLOptions aplOptions) {
        mAPLOptions = aplOptions;
    }

    public void startFrameMetricsRecording(int id, IDTCallback<String> callback) {
        mView.post(() -> mView.startFrameMetricsRecording(id, callback));
    }

    public void stopFrameMetricsRecording(int id, IDTCallback<List<JSONObject>> callback) {
        mView.post(() -> mView.stopFrameMetricsRecording(id, callback));
    }

    public void getPerformanceMetrics(int id, IDTCallback<List<MetricInfo>> callback) {
        if (mAPLOptions == null) {
            callback.execute(RequestStatus.failed(id, DTError.NO_PERFORMANCE_METRICS));
        }

        ITelemetryProvider telemetryProvider = mAPLOptions.getTelemetryProvider();

        // TODO: Update when the refactoring is completed. Jira:ELON-40529
        if (telemetryProvider instanceof LoggingTelemetryProvider) {
            post(() -> {
                List<MetricInfo> result = ((LoggingTelemetryProvider) telemetryProvider).getPerformanceMetrics();
                callback.execute(result, RequestStatus.successful());
            });
        } else {
            post(() -> callback.execute(RequestStatus.failed(id, DTError.NO_PERFORMANCE_METRICS)));
        }
    }

    public void getCurrentBitmap(IDTCallback<Bitmap> callback) {
        post(() -> callback.execute(mCaptureImageHelper.captureImage((View) mView), RequestStatus.successful()));
    }

    public void executeCommands(String commands, IDTCallback<ExecuteCommandStatus> callback) {
        mView.executeCommands(commands, callback);
    }

    public void updateLiveData(String name, List<LiveData.Update> operations, IDTCallback<Boolean> callback) {
        mView.post(() -> mView.updateLiveData(name, operations, callback));
    }

    public void onViewStateChange(ViewState viewState, double timestamp) {
        mCurrentDocumentView = viewState;
        for (Session session : getRegisteredSessions()) {
            session.sendEvent(new ViewStateChangeEvent(session.getSessionId(), viewState,
                    mCurrentDocumentId, timestamp));
        }

        if (mCurrentDocumentView == ViewState.READY) {
            sendPerformanceMetricsEvent();
        }
    }

    private void sendPerformanceMetricsEvent() {
        post(() -> {
            if (mAPLOptions != null && mAPLOptions.getTelemetryProvider() instanceof  LoggingTelemetryProvider) {
                List<MetricInfo> result = ((LoggingTelemetryProvider) mAPLOptions.getTelemetryProvider()).getPerformanceMetrics();
                for (Session session : getRegisteredSessions()) {
                    if (session.isPerformanceEnabled()) {
                        session.sendEvent(new PerformanceMetricsEvent(session.getSessionId(), result));
                    }
                }
            }
        });
    }

    public void onLogEntryAdded(com.amazon.apl.android.Session.LogEntryLevel level, com.amazon.apl.android.Session.LogEntrySource source, String messageText, double timestamp, Object[] arguments) {
        LogEntry entry = new LogEntry(level, source, messageText, timestamp, arguments);
        mLogEntries.add(entry);
        for (Session session : getRegisteredSessions()) {
            if (session.isLogEnabled()) {
                session.sendEvent(new LogEntryAddedEvent(session.getSessionId(), level, source, messageText,
                        timestamp, arguments));
            }
        }
    }

    @Override
    public void write(com.amazon.apl.android.Session.LogEntryLevel level, com.amazon.apl.android.Session.LogEntrySource source, String message, Object[] arguments) {
        post(()-> onLogEntryAdded(level, source, message, System.currentTimeMillis() / 1000D, arguments));
    }

    public void onGenerateNewDocumentId() {
        mCurrentDocumentId = mIdGenerator.generateId();
    }

    public int getCurrentDocumentId() {
        return mCurrentDocumentId;
    }

    public void setCurrentDocumentId(int documentId) {
        mCurrentDocumentId = documentId;
    }

    public void clearLog() {
        mLogEntries.clear();
    }

    public List<LogEntry> getLogEntries() {
        return mLogEntries;
    }

    public void cleanup() {
        // Remove any remaining messages in the queue
        mHandler.removeCallbacksAndMessages(null);
    }

    public void documentCommandRequest(int id, String method, JSONObject params, IDTCallback<String> callback) {
        mView.documentCommandRequest(id, method, params, callback);
    }

    public void onNetworkRequestWillBeSent(int requestId,  double timestamp,
                                           String documentURL, String type) {
        for (Session session : getRegisteredSessions()) {
            if (session.isNetworkEnabled()){
                session.sendEvent(new NetworkRequestWillBeSentEvent(session.getSessionId(), requestId,
                        timestamp, documentURL, type));
            }
        }
    }

    public void onNetworkLoadingFailed(int requestId,  double timestamp) {
        for (Session session : getRegisteredSessions()) {
            if (session.isNetworkEnabled()) {
                session.sendEvent(new NetworkLoadingFailedEvent(session.getSessionId(), requestId, timestamp));
            }
        }
    }

    public void onNetworkLoadingFinished(int requestId, double timestamp, int encodedDataLength) {
        for (Session session : getRegisteredSessions()) {
            if (session.isNetworkEnabled()) {
                session.sendEvent(new NetworkLoadingFinishedEvent(session.getSessionId(), requestId,
                        timestamp, encodedDataLength));
            }
        }
    }

    public IDTNetworkRequestHandler getDTNetworkRequestHandler() {
        return mDTNetworkRequestHandler;
    }
}
