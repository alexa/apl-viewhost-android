/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models;

import android.graphics.Bitmap;
import android.os.Handler;

import android.view.View;
import android.view.MotionEvent;
import androidx.annotation.VisibleForTesting;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.LiveData;
import com.amazon.apl.android.dependencies.IAPLSessionListener;
import com.amazon.apl.android.providers.impl.LoggingTelemetryProvider;
import com.amazon.apl.android.utils.MetricInfo;
import com.amazon.apl.developer.views.CaptureImageHelper;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.enums.TargetType;
import com.amazon.apl.devtools.enums.ViewState;
import com.amazon.apl.devtools.models.log.LogEntry;
import com.amazon.apl.devtools.models.log.LogEntryAddedEvent;
import com.amazon.apl.devtools.models.network.NetworkLoadingFailedEvent;
import com.amazon.apl.devtools.models.network.NetworkLoadingFinishedEvent;
import com.amazon.apl.devtools.models.network.NetworkRequestWillBeSentEvent;
import com.amazon.apl.devtools.models.performance.IMetricsService;
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
    private static final String TARGET_NAME = "main";
    private IAPLView mView;
    private final Handler mHandler;
    private final List<LogEntry> mLogEntries = new ArrayList<>();
    private final IdGenerator mIdGenerator = new IdGenerator();
    private int mCurrentDocumentId = 0;
    private ViewState mCurrentDocumentViewState = ViewState.EMPTY;

    // The metric revamp is fairly new and to avoid blocking any pipeline/QA testing
    // due to missing metrics when running the automationapp. We will keep the old approach for now
    // and remove later once we have all metrics inplace.
    // TODO: remove APLOptions dependency.
    private APLOptions mAPLOptions;
    private IMetricsService mMetricRetriever;

    private final CaptureImageHelper mCaptureImageHelper = new CaptureImageHelper();

    public ViewTypeTarget() {
        this(DependencyContainer.getInstance().getTargetCatalog().getHandler());
    }

    @VisibleForTesting
    ViewTypeTarget(final Handler handler) {
        super(TargetType.VIEW, TARGET_NAME);
        mHandler = handler;
    }

    public void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    public void setView(IAPLView aplViewLayout) {
        mView = aplViewLayout;
    }

    public void setMetricsRetriever(final IMetricsService metricsRetriever) {
        mMetricRetriever = metricsRetriever;
    }

    public void addLiveData(String name, LiveData data) {
        mView.addLiveData(name, data);
    }


    @Override
    public void registerSession(Session session) {
        super.registerSession(session);

        final double timestamp = System.currentTimeMillis() / 1000D;
        post(() -> reportLatestViewState(session, timestamp));
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

    public float getDisplayRefreshRate() {
        return mView.getDisplayRefreshRate();
    }

    public void getPerformanceMetrics(int id, IDTCallback<List<MetricInfo>> callback) {
        post(() -> {
            List<MetricInfo> result = null;
            // TODO: Update to use MetricRetrieve when we have all metrics implemented with the revamp.
            if (mAPLOptions != null && mAPLOptions.getTelemetryProvider() instanceof LoggingTelemetryProvider) {
                result = ((LoggingTelemetryProvider) mAPLOptions.getTelemetryProvider()).getPerformanceMetrics();
            } else if (mMetricRetriever != null){
                result = mMetricRetriever.retrieveMetrics();
            }
            if (result != null) {
                callback.execute(result, RequestStatus.successful());
            } else {
                callback.execute(RequestStatus.failed(id, DTError.NO_PERFORMANCE_METRICS));
            }
        });
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

    public void requestScenegraph(IDTCallback<String> callback) {
        /* to do: implement */
    }

    public void requestVisualContext(IDTCallback<String> callback) {
        /* to do: implement */
    }

    public void requestDOM(IDTCallback<String> callback) {
        /* to do: implement */
    }

    public void enqueueInputEvents(int id, List<MotionEvent> events, IDTCallback<Boolean> callback) {
        mView.post(() -> mView.enqueueMotionEvents(events, callback));
    }

    public void clearInputEvents() {
        mView.post(() -> mView.clearMotionEvents());
    }

    public void onViewStateChange(ViewState viewState, double timestamp) {
        mCurrentDocumentViewState = viewState;
        for (Session session : getRegisteredSessions()) {
            reportLatestViewState(session, timestamp);
        }

        if (ViewState.READY.equals(viewState)) {
            post(this::sendPerformanceMetricsEvent);
        }

        if (ViewState.EMPTY.equals(viewState)) {
            if (mMetricRetriever != null) {
                mMetricRetriever.clearMetrics();
            }
        }
    }

    private void reportLatestViewState(final Session session, final double timestamp) {
        session.sendEvent(new ViewStateChangeEvent(session.getSessionId(), mCurrentDocumentViewState,
                mCurrentDocumentId, timestamp));
    }

    private void sendPerformanceMetricsEvent() {
        List<MetricInfo> result = null;
        if (mAPLOptions != null && mAPLOptions.getTelemetryProvider() instanceof  LoggingTelemetryProvider) {
            result = ((LoggingTelemetryProvider) mAPLOptions.getTelemetryProvider()).getPerformanceMetrics();
        } else if (mMetricRetriever != null) {
            result = mMetricRetriever.retrieveMetrics();
        }
        if (result != null) {
            for (Session session : getRegisteredSessions()) {
                if (session.isPerformanceEnabled()) {
                    session.sendEvent(new PerformanceMetricsEvent(session.getSessionId(), result));
                }
            }
        }
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
        mView.post(() -> mView.documentCommandRequest(id, method, params, callback));
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
}
