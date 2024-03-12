/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import com.amazon.apl.devtools.enums.DTNetworkRequestType;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * This manager class is to correctly route the NetworkEvents to the DTNetworkRequestHandler
 * because {@link ViewhostImpl#prepare(PrepareDocumentRequest)} does not guarantee that the APLLayout
 * will be bind at that point before making the Network calls to import the packages.
 */
class DTNetworkRequestManager implements IDTNetworkRequestHandler {
    private final List<NetworkRequestInfo> mNetworkRequests;
    private IDTNetworkRequestHandler mDTNetworkRequestHandler;
    private enum EventType {
        REQUEST_WILL_BE_SENT,
        LOADING_FINISHED,
        LOADING_FAILED
    }

    DTNetworkRequestManager() {
        mNetworkRequests = new ArrayList<>();
    }


    @Override
    public void requestWillBeSent(int requestId, double timestamp, String url, DTNetworkRequestType type) {
        if (mDTNetworkRequestHandler == null) {
            NetworkRequestInfo networkRequestInfo = new NetworkRequestInfo(requestId, timestamp, EventType.REQUEST_WILL_BE_SENT).setUrl(url);
            mNetworkRequests.add(networkRequestInfo);
            return;
        }

        mDTNetworkRequestHandler.requestWillBeSent(requestId, timestamp, url, type);
    }

    @Override
    public void loadingFailed(int requestId, double timestamp) {
        if (mDTNetworkRequestHandler == null) {
            mNetworkRequests.add(new NetworkRequestInfo(requestId, timestamp, EventType.LOADING_FAILED));
            return;
        }

        mDTNetworkRequestHandler.loadingFailed(requestId, timestamp);
    }

    @Override
    public void loadingFinished(int requestId, double timestamp, int encodedDataLength) {
        if (mDTNetworkRequestHandler == null) {
            mNetworkRequests.add(new NetworkRequestInfo(requestId, timestamp, EventType.LOADING_FINISHED)
                    .setEncodedDataLength(encodedDataLength));
            return;
        }

        mDTNetworkRequestHandler.loadingFinished(requestId, timestamp, encodedDataLength);
    }

    public void bindDTNetworkRequest(IDTNetworkRequestHandler dtNetworkRequestHandler) {
        mDTNetworkRequestHandler = dtNetworkRequestHandler;
        reportAllNetworkEvents();
    }

    public void unbindDTNetworkRequest() {
        mDTNetworkRequestHandler = null;
    }

    private void reportAllNetworkEvents() {
        for (NetworkRequestInfo networkRequestInfo : mNetworkRequests) {
            int requestId = networkRequestInfo.getRequestId();
            double timestamp = networkRequestInfo.getTimestamp();
            String url = networkRequestInfo.getUrl();
            EventType eventType = networkRequestInfo.getEventType();

            if (EventType.REQUEST_WILL_BE_SENT.equals(eventType)) {
                mDTNetworkRequestHandler.requestWillBeSent(requestId, timestamp, url, DTNetworkRequestType.PACKAGE);
            } else if (EventType.LOADING_FINISHED.equals(eventType)) {
                mDTNetworkRequestHandler.loadingFinished(requestId, timestamp, networkRequestInfo.getEncodedDataLength());
            } else {
                mDTNetworkRequestHandler.loadingFailed(requestId, timestamp);
            }
        }
        mNetworkRequests.clear();
    }

    private static class NetworkRequestInfo {
        private final int mRequestId;
        private final double mTimestamp;
        private final EventType mEventType;
        private String mUrl;
        private int mEncodedDataLength;

        NetworkRequestInfo(int requestId, double timestamp, EventType eventType) {
            mRequestId = requestId;
            mTimestamp = timestamp;
            mEventType = eventType;
        }

        public int getRequestId() {
            return mRequestId;
        }

        public double getTimestamp() {
            return mTimestamp;
        }

        public String getUrl() {
            return mUrl;
        }

        public EventType getEventType() {
            return mEventType;
        }

        public int getEncodedDataLength() {
            return mEncodedDataLength;
        }


        public NetworkRequestInfo setUrl(String url) {
            mUrl = url;
            return this;
        }

        public NetworkRequestInfo setEncodedDataLength(int encodedDataLength) {
            mEncodedDataLength = encodedDataLength;
            return this;
        }
    }
}
