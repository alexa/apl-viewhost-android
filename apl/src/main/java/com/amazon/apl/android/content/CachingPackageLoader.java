/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.dependencies.IPackageCache;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.google.auto.value.AutoValue;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;

/**
 * Class to handle multiple requests for packages across APL Documents using a Cache and delegate.
 *
 * Handles queuing up requests for the same set of imports. Previously this was handled by Core internally
 * but if we have several Contents that are requesting the same imports simultaneously we should return
 * the same json object.
 */
public class CachingPackageLoader implements IPackageLoader {
    private final IPackageLoader mDelegate;
    private final IPackageCache mPackageCache;
    private final ITelemetryProvider mTelemetryProvider;

    private final Map<Content.ImportRef, Queue<Request>> mPendingRequestMap = new ConcurrentHashMap<>();
    private int cPackageMemoryCacheHit;
    private int cPackageMemoryCacheSize;

    private static final String METRIC_PACKAGE_MEMORY_CACHE_HIT = "PackageMemoryCacheHit";
    private static final String METRIC_PACKAGE_MEMORY_CACHE_SIZE = "PackageMemoryCacheSize";


    public CachingPackageLoader(IPackageLoader delegateLoader, IPackageCache packageCache) {
        mDelegate = delegateLoader;
        mPackageCache = packageCache;
        mTelemetryProvider = null;
    }

    public CachingPackageLoader(IPackageLoader delegateLoader, IPackageCache packageCache, ITelemetryProvider telemetryProvider) {
        mDelegate = delegateLoader;
        mPackageCache = packageCache;
        mTelemetryProvider = telemetryProvider;
        cPackageMemoryCacheHit = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_PACKAGE_MEMORY_CACHE_HIT, COUNTER);
        cPackageMemoryCacheSize = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_PACKAGE_MEMORY_CACHE_SIZE, COUNTER );
    }

    @Override
    public synchronized void fetch(@NonNull Content.ImportRequest request, @NonNull SuccessCallback<Content.ImportRequest, APLJSONData> successCallback, @NonNull FailureCallback<Content.ImportRequest> failureCallback) {
        final Content.ImportRef ref = request.getImportRef();

        APLJSONData cachedPackage = mPackageCache.get(ref);
        if (cachedPackage != null) {
            successCallback.onSuccess(request, cachedPackage);
            if(mTelemetryProvider != null) {
                mTelemetryProvider.incrementCount(cPackageMemoryCacheHit);
            }
            return;
        }

        // Send to delegate
        Queue<Request> pendingRequests = mPendingRequestMap.get(ref);
        final Request pendingRequest = Request.create(request, successCallback, failureCallback);
        if (pendingRequests == null) {
            pendingRequests = new ConcurrentLinkedQueue<>();
            mPendingRequestMap.put(ref, pendingRequests);
            pendingRequests.add(pendingRequest);
            mDelegate.fetch(request,
                    (Content.ImportRequest innerRequest, APLJSONData apljsonData) ->
                            handleResponse(Response.create(innerRequest, apljsonData, null)),
                    (@NonNull Content.ImportRequest innerRequest, @NonNull String failMessage) ->
                            handleResponse(Response.create(innerRequest, null, failMessage))
            );
        } else {
            pendingRequests.add(pendingRequest);
        }
    }

    private synchronized void handleResponse(Response response) {
        final Content.ImportRequest importRequest = response.request();
        final Content.ImportRef importRef = importRequest.getImportRef();
        Queue<Request> pendingRequests = mPendingRequestMap.remove(importRef);
        if (pendingRequests == null) {
            return;
        }

        Request pending;
        while ((pending = pendingRequests.poll()) != null) {
            final Content.ImportRequest pendingRequest = pending.request();
            final APLJSONData aplJsonData = response.apljsonData();
            final String failMessage = response.failMessage();
            if (aplJsonData != null) {
                mPackageCache.put(importRef, aplJsonData);
                if(mTelemetryProvider != null) {
                    mTelemetryProvider.incrementCount(cPackageMemoryCacheSize, mPackageCache.getSize());
                }
                pending.successCallback().onSuccess(pendingRequest, aplJsonData);
            } else {
                pending.failureCallback().onFailure(pendingRequest, failMessage);
            }
        }
    }

    /**
     * Simple object containing the request and callbacks for success/failure.
     */
    @AutoValue
    static abstract class Request {
        abstract Content.ImportRequest request();
        abstract SuccessCallback<Content.ImportRequest, APLJSONData> successCallback();
        abstract FailureCallback<Content.ImportRequest> failureCallback();
        public static Request create(final Content.ImportRequest request,
                              final SuccessCallback<Content.ImportRequest, APLJSONData> successCallback,
                              final FailureCallback<Content.ImportRequest> failureCallback) {
            return new AutoValue_CachingPackageLoader_Request(request, successCallback, failureCallback);
        }
    }

    /**
     * Simple object containing the response from the delegate.
     */
    @AutoValue
    static abstract class Response {
        abstract Content.ImportRequest request();
        @Nullable
        abstract APLJSONData apljsonData();
        @Nullable
        abstract String failMessage();
        public static Response create(final Content.ImportRequest request,
                               final APLJSONData apljsonData,
                               final String failMessage) {
            return new AutoValue_CachingPackageLoader_Response(request, apljsonData, failMessage);
        }
    }
}
