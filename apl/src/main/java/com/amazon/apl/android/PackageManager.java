/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.COUNTER;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.common.BoundObject;

public class PackageManager extends BoundObject {

    private static final String TAG = "PackageManager";
    public static final String METRIC_CONTENT_IMPORT_REQUESTS = TAG + ".imports";
    public static final String METRIC_CONTENT_ERROR = TAG + ".error";

    @Nullable
    private final IDTNetworkRequestHandler mDTNetworkRequestHandler;

    private IPackageLoader mPackageLoader;

    @NonNull
    private final ITelemetryProvider mTelemetryProvider;

    private int cPackageImportRequests, cContentError;

    @NonNull
    private final Handler mMainHandler;


    public PackageManager(IPackageLoader loader, ITelemetryProvider telemetryProvider, IDTNetworkRequestHandler networkRequestHandler) {
        bind(nCreate());
        mTelemetryProvider = telemetryProvider;
        mDTNetworkRequestHandler = networkRequestHandler;
        mPackageLoader = loader;
        mMainHandler = new Handler(Looper.getMainLooper());

        cPackageImportRequests = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_IMPORT_REQUESTS, COUNTER);
        cContentError = mTelemetryProvider.createMetricId(APL_DOMAIN, METRIC_CONTENT_ERROR, COUNTER);
    }
    private void coreRequestPackage(long packageRequestHandle, long importRequestHandle, String source, String name, String version, String domain) {
        mTelemetryProvider.incrementCount(cPackageImportRequests);
        Content.ImportRequest importRequest = new Content.ImportRequest(importRequestHandle, source, name, version, domain, IDTNetworkRequestHandler.IdGenerator.generateId());
        PackageRequest packageRequest = new PackageRequest(packageRequestHandle);

        final long startTime = SystemClock.elapsedRealtimeNanos();

        mPackageLoader.fetchV2(importRequest,
                (Content.ImportRequest req, APLJSONData result) -> this.handleImportSuccess(importRequest, packageRequest, result, startTime),
                (Content.ImportRequest req, String message, int code) -> {
                    final long duration = NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startTime);
                    Log.e(TAG, String.format("Unable to load content for request: %s. Failed in %d milliseconds. %s",
                            importRequest,
                            duration,
                            message));
                    if (mDTNetworkRequestHandler != null && IDTNetworkRequestHandler.isUrlRequest(source)) {
                        mDTNetworkRequestHandler.loadingFailed(importRequest.getRequestId(), SystemClock.elapsedRealtimeNanos());
                    }
                    mTelemetryProvider.incrementCount(cContentError);
                    invokeOnMyThread(() -> failure(packageRequest, code, message));
                }
        );
    }

    private void invokeOnMyThread(Runnable runnable) {
        if (Thread.currentThread() == mMainHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            mMainHandler.post(runnable);
        }
    }

    private void handleImportSuccess(Content.ImportRequest importRequest, PackageRequest packageRequest, APLJSONData result, final long startTime) {
        final long duration = NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - startTime);
        Log.i(TAG, String.format("Package '%s' took %d milliseconds to download.",
                importRequest.getPackageName(),
                duration));

        final String source = importRequest.getSource();
        if (mDTNetworkRequestHandler != null && IDTNetworkRequestHandler.isUrlRequest(source)) {
            mDTNetworkRequestHandler.loadingFinished(importRequest.getRequestId(), SystemClock.elapsedRealtimeNanos(), result.getSize());
        }
        invokeOnMyThread(() -> success(packageRequest, result));
    }

    private void success(PackageRequest request, APLJSONData result) {
        nSuccess(request.getNativeHandle(), result.getNativeHandle());
    }
    private void failure(PackageRequest request, int code, String message) {
        nFailure(request.getNativeHandle(), code, message);
    }

    private native void nSuccess(long packageRequestHandle, long sharedDataHandle);
    private native void nFailure(long packageRequestHandle, int errorCode, String errorMessage);
    private native long nCreate();

}
